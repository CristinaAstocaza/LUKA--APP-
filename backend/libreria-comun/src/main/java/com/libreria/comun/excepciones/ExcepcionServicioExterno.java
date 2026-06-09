package com.libreria.comun.excepciones;

import com.libreria.comun.enums.CodigoError;
import java.util.Map;

/**
 * ExcepciÃ³n lanzada cuando ocurre un error en la comunicaciÃ³n con un servicio externo
 * (ej. Fallo en el microservicio de IA, mensajerÃ­a o API de terceros).
 * <p>Mapea a un estado HTTP 502 Bad Gateway.</p>
 * 
 */
public class ExcepcionServicioExterno extends ExcepcionGlobal {

    /**
     * @param servicio Nombre del servicio que fallÃ³.
     * @param razon    Detalle tÃ©cnico del error devuelto por el servicio externo.
     */
    public ExcepcionServicioExterno(String servicio, String razon) {
        super(CodigoError.ERROR_SERVICIO_EXTERNO, 
              "Error crÃ­tico al invocar el servicio: " + servicio, 
              Map.of("servicio", servicio, "razon", razon));
    }
}
