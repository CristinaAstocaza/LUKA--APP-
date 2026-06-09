package com.mensajeria.aplicacion.servicios;

import com.mensajeria.aplicacion.excepciones.MensajeriaExternaException;
import com.mensajeria.aplicacion.puertos.IWhatsAppService;
import com.mensajeria.aplicacion.servicios.canales.CanalNotificacionStrategy;
import com.mensajeria.aplicacion.servicios.canales.TipoNotificacion;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import com.mensajeria.infraestructura.configuracion.PropiedadesTwilio;

import java.util.Map;

/**
 * ImplementaciÃ³n del servicio de WhatsApp usando Twilio.
 *
 * <p>A partir de la v1.2.0 el envÃ­o se realiza a travÃ©s del
 * <strong>Messaging Service SID</strong> ({@code TWILIO_MESSAGING_SERVICE_SID}),
 * eliminando la dependencia de un nÃºmero de WhatsApp fijo como origen.
 * Twilio gestiona el enrutamiento internamente.</p>
 *
 * @version 1.2.0
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class WhatsAppServiceImpl implements IWhatsAppService, CanalNotificacionStrategy {

    private final PropiedadesTwilio propiedadesTwilio;

    @Override
    public void enviar(String destinatario, Map<String, Object> variables) {
        String codigo = (String) variables.get("codigo");
        this.sendVerificationCode(destinatario, codigo);
    }

    @Override
    public boolean soporta(TipoNotificacion tipo) {
        return tipo == TipoNotificacion.WHATSAPP;
    }

    /**
     * EnvÃ­a un cÃ³digo de verificaciÃ³n por WhatsApp aprovechando la ventana de 24 horas.
     *
     * <p>Utiliza {@code MessagingServiceSid} si estÃ¡ configurado (producciÃ³n);
     * de lo contrario, cae de forma degradada al nÃºmero estÃ¡tico {@code whatsapp.from}
     * para entornos locales sin Messaging Service.</p>
     *
     * @param targetPhone NÃºmero del cliente en formato internacional (ej: {@code +51943455686})
     * @param token       CÃ³digo de verificaciÃ³n generado por el sistema
     * @return El SID del mensaje generado por Twilio si el envÃ­o fue exitoso
     */
    public String sendVerificationCode(String targetPhone, String token) {
        if (!esNumeroValido(targetPhone)) {
            log.error("[WHATSAPP] Formato de telÃ©fono invÃ¡lido: {}. Se requiere E.164 (ej. +51943455686)", targetPhone);
            throw new com.mensajeria.aplicacion.excepciones.TelefonoInvalidoException(
                "El nÃºmero " + targetPhone + " no tiene el formato internacional requerido."
            );
        }

        try {
            String messageBody = String.format(
                "Luka App: Tu cÃ³digo de verificaciÃ³n es [%s]. Expira en 5 minutos. No lo compartas con nadie.",
                token
            );

            String formattedPhone = targetPhone.startsWith("+") ? targetPhone : "+" + targetPhone;
            // Twilio requiere el prefijo "whatsapp:" en el nÃºmero destino
            String whatsappDest = "whatsapp:" + formattedPhone;

            String sandboxWhatsappFrom = propiedadesTwilio.getSandboxWhatsappFrom();
            String messagingServiceSid = propiedadesTwilio.getMessagingServiceSid();
            Message message;

            if (StringUtils.hasText(sandboxWhatsappFrom)) {
                String fromWhatsapp = "whatsapp:" + sandboxWhatsappFrom;
                log.info("[WHATSAPP] Usando Sandbox con remitente explÃ­cito: {}", fromWhatsapp);
                message = Message.creator(
                        new PhoneNumber(whatsappDest),
                        new PhoneNumber(fromWhatsapp),
                        messageBody
                ).create();
            } else if (StringUtils.hasText(messagingServiceSid)) {
                // Modo producciÃ³n: Messaging Service gestiona el nÃºmero de origen
                log.info("[WHATSAPP] Usando MessagingServiceSid: {}", messagingServiceSid);
                message = Message.creator(
                        new PhoneNumber(whatsappDest),
                        messagingServiceSid,
                        messageBody
                ).create();
            } else {
                // Modo fallback: nÃºmero de WhatsApp estÃ¡tico (desarrollo local)
                String fromWhatsapp = "whatsapp:" + propiedadesTwilio.getWhatsapp().getFrom();
                log.warn("[WHATSAPP] MessagingServiceSid no configurado. Usando nÃºmero estÃ¡tico: {}", fromWhatsapp);
                message = Message.creator(
                        new PhoneNumber(whatsappDest),
                        new PhoneNumber(fromWhatsapp),
                        messageBody
                ).create();
            }

            log.info("[WHATSAPP] Mensaje de verificaciÃ³n enviado con Ã©xito. SID: {}", message.getSid());
            return message.getSid();

        } catch (Exception e) {
            log.error("[WHATSAPP] Error al enviar el token de validaciÃ³n por WhatsApp a {}: {}", targetPhone, e.getMessage());
            throw new MensajeriaExternaException("Fallo en el envÃ­o de la notificaciÃ³n de seguridad por WhatsApp", e.getMessage());
        }
    }

    @Override
    public void enviarMensajeTemplate(String telefono, String plantilla, Map<String, String> variables) {
        // Como ya no se usan plantillas, redirigimos la lÃ³gica a sendVerificationCode.
        // Asumimos que el token es la primera variable (ej. "1").
        String token = variables != null ? variables.getOrDefault("1", "DESCONOCIDO") : "DESCONOCIDO";
        this.sendVerificationCode(telefono, token);
    }

    @Override
    public boolean esNumeroValido(String telefono) {
        // WhatsApp requiere formato E.164 (ej: +51943455686) para Twilio
        return telefono != null && telefono.matches("^\\+?[1-9]\\d{9,14}$");
    }
}
