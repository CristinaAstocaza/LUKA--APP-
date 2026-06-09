package com.mensajeria.aplicacion.servicios.canales;

import java.util.Map;

/**
 * Contrato unificado del servicio de notificaciones del microservicio de mensajerÃ­a.
 * <p>
 * Esta interfaz es <strong>agnÃ³stica al canal</strong>: no sabe si el mensaje
 * se enviarÃ¡ por email o SMS. Cada implementaciÃ³n concreta en
 * {@code com.mensajeria.aplicacion.servicios.impl} decide cÃ³mo despachar la
 * notificaciÃ³n segÃºn el {@link TipoNotificacion} recibido.
 * </p>
 * <p>
 * El mapa {@code variables} es abierto por diseÃ±o: permite que cada
 * implementaciÃ³n extraiga lo que necesita ({@code "codigo"}, {@code "appName"},
 * {@code "proposito"}, etc.) sin acoplar la interfaz a DTOs concretos.
 * </p>
 *
 * @version 1.1.0
 */
public interface NotificacionService {

    /**
     * EnvÃ­a una notificaciÃ³n al destinatario usando el canal y las variables
     * proporcionadas.
     *
     * @param tipo         Canal de envÃ­o ({@link TipoNotificacion#EMAIL} o
     *                     {@link TipoNotificacion#SMS}). La implementaciÃ³n
     *                     deberÃ¡ lanzar {@link UnsupportedOperationException}
     *                     si no soporta el tipo recibido.
     * @param destinatario DirecciÃ³n de destino: email en formato RFC 5321 para
     *                     {@code EMAIL}, o nÃºmero en formato E.164 para {@code SMS}.
     * @param variables    Mapa de parÃ¡metros de la plantilla. Claves estÃ¡ndar:<br>
     *                     &nbsp;â€¢ {@code "codigo"} â€” OTP de 6 dÃ­gitos (requerido).<br>
     *                     &nbsp;â€¢ {@code "proposito"} â€” {@code PropositoCodigo} del OTP.<br>
     *                     &nbsp;â€¢ {@code "appName"} â€” nombre de la app para el asunto del correo.
     * @throws com.mensajeria.aplicacion.excepciones.MensajeriaExternaException
     *             si el proveedor externo (SMTP, Twilio) rechaza el envÃ­o.
     */
    void enviar(TipoNotificacion tipo, String destinatario, Map<String, Object> variables);

}
