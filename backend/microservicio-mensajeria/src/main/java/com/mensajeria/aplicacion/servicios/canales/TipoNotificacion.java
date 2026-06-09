package com.mensajeria.aplicacion.servicios.canales;

/**
 * EnumeraciÃ³n de los canales de notificaciÃ³n disponibles en el sistema.
 * <p>
 * Permite que {@link NotificacionService} sea completamente agnÃ³stico al
 * medio de entrega: la implementaciÃ³n concreta decide cÃ³mo enviar el mensaje
 * (SMTP, Twilio, etc.) segÃºn el tipo recibido.
 * </p>
 *
 * @version 1.1.0
 */
public enum TipoNotificacion {

    /**
     * NotificaciÃ³n enviada por correo electrÃ³nico mediante SMTP (Thymeleaf).
     * La clave {@code "destinatario"} del mapa de variables debe contener el email.
     */
    EMAIL,

    /**
     * NotificaciÃ³n enviada por SMS mediante Twilio.
     * La clave {@code "destinatario"} del mapa de variables debe contener el
     * nÃºmero de telÃ©fono en formato E.164.
     */
    SMS,

    /**
     * NotificaciÃ³n enviada por WhatsApp mediante Meta Cloud API.
     */
    WHATSAPP
}
