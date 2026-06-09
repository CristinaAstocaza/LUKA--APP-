package com.mensajeria.aplicacion.excepciones;

import com.libreria.comun.enums.CodigoError;
import com.libreria.comun.excepciones.ExcepcionGlobal;

/**
 * ExcepciÃ³n lanzada por el {@code ServicioThrottling} cuando un usuario supera
 * el lÃ­mite mÃ¡ximo de intentos de envÃ­o por canal (EMAIL o SMS).
 * <p>
 * El manejador global de la librerÃ­a la captura automÃ¡ticamente y devuelve
 * HTTP 429 con el cÃ³digo semÃ¡ntico {@code LIMITE_DIARIO_EXCEDIDO}.
 * </p>
 *
 * @version 1.1.0
 */
public class LimiteIntentosExcedidoException extends ExcepcionGlobal {

    /**
     * Construye la excepciÃ³n indicando el canal bloqueado para informar al usuario
     * quÃ© medio de notificaciÃ³n ha quedado suspendido hasta la medianoche.
     *
     * @param canal El canal de notificaciÃ³n bloqueado (ej. {@code "email"} o
     *              {@code "sms"}).
     */
    public LimiteIntentosExcedidoException(String canal) {
        super(
            CodigoError.LIMITE_DIARIO_EXCEDIDO,
            String.format(
                "Ha superado el lÃ­mite de intentos de envÃ­o por %s. El contador se reinicia a las 00:00:00.",
                canal.toUpperCase()
            ),
            null
        );
    }
}
