package com.auditoria.aplicacion.puertos;

import com.auditoria.aplicacion.dtos.RespuestaAuditoriaDetalladoDTO;
import com.libreria.comun.dtos.EventoAuditoriaDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Interfaz de puerto para el registro y consulta de eventos de auditorÃ­a
 * general.
 * <p>
 * Define el contrato para persistir trazas de actividad de usuario y
 * realizar consultas histÃ³ricas filtradas para el ecosistema Luka App.
 * </p>
 * 
 * @since 2026-05
 */
public interface ServicioRegistroAuditoria {

    /**
     * Persiste un nuevo evento de auditorÃ­a en la base de datos.
     * <p>
     * Se utiliza {@link EventoAuditoriaDTO} como contrato unificado para
     * capturar la actividad proveniente de cualquier microservicio.
     * </p>
     * 
     * @param request Datos del evento (acciÃ³n, mÃ³dulo, IP, etc.).
     * @return {@link EventoAuditoriaDTO} con los datos persistidos e ID generado.
     */
    EventoAuditoriaDTO registrarEvento(EventoAuditoriaDTO request);

    /**
     * Recupera el historial de auditorÃ­a enriquecido con datos de usuario.
     * 
     * @param modulo     Filtro opcional por microservicio.
     * @param paginacion Metadatos de paginaciÃ³n.
     * @return PÃ¡gina de DTOs detallados para la interfaz de usuario.
     */
    Page<RespuestaAuditoriaDetalladoDTO> listarRegistrosDetallados(String modulo, Pageable paginacion);
}
