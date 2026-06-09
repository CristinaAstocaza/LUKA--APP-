package com.mensajeria.aplicacion.servicios;

import com.mensajeria.aplicacion.puertos.ISmsService;
import com.mensajeria.aplicacion.servicios.canales.CanalNotificacionStrategy;
import com.mensajeria.aplicacion.servicios.canales.TipoNotificacion;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.lang.NonNull;

import com.mensajeria.infraestructura.configuracion.PropiedadesTwilio;

/**
 * ImplementaciÃ³n concreta de {@link ISmsService} que usa el SDK de Twilio.
 *
 * <p>A partir de la v1.2.0 el origen del mensaje se gestiona a travÃ©s del
 * <strong>Messaging Service SID</strong> ({@code TWILIO_MESSAGING_SERVICE_SID}),
 * eliminando la dependencia de un nÃºmero fijo. Twilio selecciona dinÃ¡micamente
 * el nÃºmero de envÃ­o con la estrategia configurada en el Messaging Service.</p>
 *
 * @version 1.2.0
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class SmsServiceImpl implements ISmsService, CanalNotificacionStrategy {

    private final PropiedadesTwilio propiedadesTwilio;

    @Override
    public void enviar(String destinatario, java.util.Map<String, Object> variables) {
        String codigo = (String) variables.get("codigo");
        this.enviarCodigoVerificacion(destinatario, codigo);
    }

    @Override
    public boolean soporta(TipoNotificacion tipo) {
        return tipo == TipoNotificacion.SMS;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Utiliza {@code MessagingServiceSid} si estÃ¡ configurado (producciÃ³n);
     * de lo contrario, cae de forma degradada al nÃºmero estÃ¡tico {@code phone.number}
     * para entornos locales sin Messaging Service.</p>
     *
     * @param telefono NÃºmero destino en formato E.164 (ej. {@code +51987654321}).
     * @param codigo   CÃ³digo OTP de 6 dÃ­gitos a enviar al usuario.
     */
    @Override
    public void enviarCodigoVerificacion(String telefono, String codigo) {
        try {
            String texto = String.format(
                "Tu cÃ³digo de verificaciÃ³n LUKA es: %s%nVÃ¡lido por 10 minutos. No lo compartas.",
                codigo
            );

            String messagingServiceSid = propiedadesTwilio.getMessagingServiceSid();
            Message msg;

            if (StringUtils.hasText(messagingServiceSid)) {
                // Modo producciÃ³n: Messaging Service gestiona el nÃºmero de origen
                log.info("[SMS] Usando MessagingServiceSid: {}", messagingServiceSid);
                msg = Message.creator(
                        new PhoneNumber(telefono),
                        messagingServiceSid,
                        texto
                ).create();
            } else {
                // Modo fallback: nÃºmero estÃ¡tico (desarrollo local)
                String fromNumber = propiedadesTwilio.getPhone().getNumber();
                log.warn("[SMS] MessagingServiceSid no configurado. Usando nÃºmero estÃ¡tico: {}", fromNumber);
                msg = Message.creator(
                        new PhoneNumber(telefono),
                        new PhoneNumber(fromNumber),
                        texto
                ).create();
            }
            log.info("[SMS] OTP enviado a {}. SID: {}", telefono, msg.getSid());
        } catch (Exception e) {
            log.error("[SMS] Error enviando OTP a {}: {}", telefono, e.getMessage());
            throw new RuntimeException("Error al enviar SMS vÃ­a Twilio: " + e.getMessage());
        }
    }

    /**
     * {@inheritDoc}
     *
     * @param telefono NÃºmero de telÃ©fono a validar.
     * @return {@code true} si sigue el patrÃ³n {@code +[cÃ³digo_paÃ­s][9-14 dÃ­gitos]}.
     */
    @Override
    public boolean esNumeroValido(String telefono) {
        return telefono != null && telefono.matches("^\\+[1-9]\\d{9,14}$");
    }
}
