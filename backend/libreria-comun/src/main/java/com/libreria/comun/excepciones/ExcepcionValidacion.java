package com.libreria.comun.excepciones;

import com.libreria.comun.enums.CodigoError;
import java.util.List;

/**
 * ExcepciÃ³n lanzada cuando los datos de entrada no cumplen con las reglas de negocio
 * o las restricciones de validaciÃ³n (@Valid).
 * <p>Mapea a un estado HTTP 400 Bad Request.</p>
 * 
 */
public class ExcepcionValidacion extends ExcepcionGlobal {

    /**
     * @param mensaje  DescripciÃ³n general del error de validaciÃ³n.
     * @param errores Lista de campos o motivos especÃ­ficos del fallo.
     */
    public ExcepcionValidacion(String mensaje, List<String> errores) {
        super(CodigoError.ERROR_VALIDACION, mensaje, errores);
    }
}
