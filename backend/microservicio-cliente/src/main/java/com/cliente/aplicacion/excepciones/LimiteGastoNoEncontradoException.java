package com.cliente.aplicacion.excepciones;

import java.util.UUID;
import com.libreria.comun.excepciones.ExcepcionRecursoNoEncontrado;

/**
 * ExcepciÃ³n lanzada cuando no se encuentra un lÃ­mite de gasto.
 * 
 * @version 1.1.0
 */
public class LimiteGastoNoEncontradoException extends ExcepcionRecursoNoEncontrado {
    public LimiteGastoNoEncontradoException(UUID usuarioId) {
        super("el lÃ­mite de gasto", usuarioId);
    }
}
