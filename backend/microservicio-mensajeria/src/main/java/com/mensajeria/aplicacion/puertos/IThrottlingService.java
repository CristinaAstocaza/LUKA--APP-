package com.mensajeria.aplicacion.puertos;

/**
 * Contrato del servicio de throttling de mensajerÃ­a.
 * <p>
 * Define la operaciÃ³n de validaciÃ³n de lÃ­mite de intentos por canal, permitiendo
 * que la implementaciÃ³n concreta en Redis sea intercambiable en testing.
 * </p>
 *
 * @version 1.1.0
 */
public interface IThrottlingService {

    /**
     * Valida y registra un intento de envÃ­o de cÃ³digo para el canal e
     * identificador dados. Si el contador acumulado en Redis supera 3, lanza
     * {@code LimiteIntentosExcedidoException}.
     *
     * @param canal         Canal de notificaciÃ³n usado ({@code "email"} o
     *                      {@code "sms"}), que forma parte de la clave Redis.
     * @param identificador Identificador Ãºnico del usuario en ese canal (email,
     *                      nÃºmero de telÃ©fono o UUID).
     * @throws com.mensajeria.aplicacion.excepciones.LimiteIntentosExcedidoException
     *             cuando el nÃºmero de intentos acumulados supera el lÃ­mite
     *             permitido para ese canal.
     */
    void registrarIntento(String canal, String identificador);
}
