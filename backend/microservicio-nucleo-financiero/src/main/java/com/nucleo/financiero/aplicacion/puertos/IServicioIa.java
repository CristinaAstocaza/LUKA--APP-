package com.nucleo.financiero.aplicacion.puertos;

import com.libreria.comun.dtos.RespuestaIaDTO;
import com.libreria.comun.dtos.SolicitudIaDTO;

/**
 * Interfaz de servicio para la interacciÃ³n con el motor de Inteligencia Artificial.
 * <p>
 * Define el contrato para solicitar anÃ¡lisis y consejos financieros basados
 * en el comportamiento del usuario.
 * </p>
 *
 * @version 1.2.0
 */
public interface IServicioIa {

    /**
     * Procesa una solicitud de consejo financiero, enriqueciÃ©ndola con contexto
     * y delegando el anÃ¡lisis al microservicio de IA.
     *
     * @param solicitud Datos bÃ¡sicos de la peticiÃ³n.
     * @param ipCliente DirecciÃ³n IP de origen para registro de auditorÃ­a.
     * @return DTO con el consejo generado y metadatos del anÃ¡lisis.
     */
    RespuestaIaDTO obtenerConsejoIA(SolicitudIaDTO solicitud, String ipCliente);
}
