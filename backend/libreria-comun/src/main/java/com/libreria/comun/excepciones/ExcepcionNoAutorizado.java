package com.libreria.comun.excepciones;

import com.libreria.comun.enums.CodigoError;
import java.util.Map;

/**
 * ExcepciÃ³n lanzada cuando el usuario no proporciona credenciales vÃ¡lidas
 * o su token JWT ha expirado/es invÃ¡lido.
 * <p>Mapea a un estado HTTP 401 Unauthorized.</p>
 * 
 */
public class ExcepcionNoAutorizado extends ExcepcionGlobal {

    /**
     * @param causa RazÃ³n especÃ­fica del rechazo (ej. "TOKEN_EXPIRADO", "TOKEN_INVALIDO").
     */
    public ExcepcionNoAutorizado(String causa) {
        super(CodigoError.ACCESO_NO_AUTORIZADO, 
              "Acceso denegado: " + causa + ". Por favor, inicie sesiÃ³n nuevamente.", 
              Map.of("causa", causa));
    }
}
