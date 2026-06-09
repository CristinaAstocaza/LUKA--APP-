package com.auditoria.aplicacion.dtos;

import java.time.LocalDate;
import java.util.UUID;

/**
 * DTO enriquecido para la visualizaciÃ³n administrativa de auditorÃ­a.
 * <p>
 * A diferencia de los eventos internos, este objeto incluye informaciÃ³n
 * de identidad (email, nombre) para facilitar la gestiÃ³n en el Dashboard.
 * </p>
 * 
 * @param id             Identificador Ãºnico del registro.
 * @param fechaHora      Instante exacto del evento.
 * @param usuarioId      UUID del usuario asociado.
 * @param emailUsuario   Correo electrÃ³nico del usuario (para identificaciÃ³n
 *                       rÃ¡pida).
 * @param nombreCompleto Nombre y apellidos del usuario.
 * @param accion         DescripciÃ³n de la operaciÃ³n realizada.
 * @param modulo         Microservicio de origen.
 * @param ipOrigen       DirecciÃ³n IP desde donde se realizÃ³ la peticiÃ³n.
 * @param detalles       InformaciÃ³n adicional o tÃ©cnica sobre el evento.
 * 
 * @since 2026-05
 */
public record RespuestaAuditoriaDetalladoDTO(
        UUID id,
        UUID usuarioId,
        String emailUsuario,
        String accion,
        String modulo,
        String ipOrigen,
        String correlationId,
        String detalles,
        LocalDate fechaHora) {
}
