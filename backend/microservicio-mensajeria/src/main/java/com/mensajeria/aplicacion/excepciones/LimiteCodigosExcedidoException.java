package com.mensajeria.aplicacion.excepciones;

import com.libreria.comun.enums.CodigoError;
import com.libreria.comun.excepciones.ExcepcionGlobal;

/**
 * ExcepciÃ³n lanzada cuando un usuario excede el lÃ­mite diario de solicitudes de
 * cÃ³digos OTP para un propÃ³sito determinado. El manejador global de la librerÃ­a
 * la captura y devuelve un HTTP 429 con el cÃ³digo semÃ¡ntico
 * {@code LIMITE_DIARIO_EXCEDIDO}.
 *
 * @version 1.1.0
 */
public class LimiteCodigosExcedidoException extends ExcepcionGlobal {

    /**
     * Construye la excepciÃ³n con un mensaje descriptivo para el usuario final.
     *
     * @param mensaje DescripciÃ³n legible del lÃ­mite superado, incluyendo cuÃ¡ndo
     *                podrÃ¡ reintentar el usuario.
     */
    public LimiteCodigosExcedidoException(String mensaje) {
        super(CodigoError.LIMITE_DIARIO_EXCEDIDO, mensaje, null);
    }
}

