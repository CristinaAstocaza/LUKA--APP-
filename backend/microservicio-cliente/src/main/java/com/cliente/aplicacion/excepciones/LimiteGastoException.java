package com.cliente.aplicacion.excepciones;

import com.libreria.comun.excepciones.ExcepcionValidacion;
import java.util.List;

/**
 * ExcepciÃ³n lanzada cuando ocurre un error de validaciÃ³n en el lÃ­mite de gasto.
 * 
 * @version 1.1.0
 */
public class LimiteGastoException extends ExcepcionValidacion {
    public LimiteGastoException(String mensaje) {
        super(mensaje, List.of(mensaje));
    }
}
