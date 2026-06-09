package com.cliente.aplicacion.puertos;

import com.cliente.aplicacion.dtos.respuestas.RespuestaDatosPersonales;
import com.cliente.aplicacion.dtos.solicitudes.SolicitudDatosPersonales;

import java.util.UUID;

/**
 * Interfaz de puerto para la gestiÃ³n de datos personales del cliente.
 * 
 * @since 2026-05
 */
public interface ServicioDatosPersonales {

    /**
     * Crea un registro vacÃ­o de datos personales vinculado al usuarioId.
     * Idempotente: si ya existe, lo devuelve sin crear uno nuevo.
     *
     * @param usuarioId ID del usuario
     * @return RespuestaDatosPersonales DTO de respuesta.
     */
    RespuestaDatosPersonales crearPerfil(UUID usuarioId);

    /**
     * Actualiza los datos personales del cliente con validaciÃ³n de propiedad.
     */
    RespuestaDatosPersonales actualizar(UUID usuarioIdRuta, UUID usuarioIdToken,
            SolicitudDatosPersonales solicitud, String ipOrigen);

    /**
     * Consulta los datos personales de un usuario, validando propiedad.
     */
    RespuestaDatosPersonales consultar(UUID usuarioIdRuta, UUID usuarioIdToken);

    /**
     * Actualiza solo el telÃ©fono del usuario (uso interno para sincronizaciÃ³n OTP).
     */
    void actualizarTelefono(UUID usuarioId, String telefono);

    /**
     * Consulta interna de datos personales sin validaciÃ³n de JWT (uso para Facade).
     */
    RespuestaDatosPersonales consultarInterno(UUID usuarioId);
}
