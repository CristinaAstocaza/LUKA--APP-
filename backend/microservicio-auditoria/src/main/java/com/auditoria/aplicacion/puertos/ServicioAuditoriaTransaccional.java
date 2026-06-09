package com.auditoria.aplicacion.puertos;

import com.libreria.comun.dtos.EventoTransaccionalDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Interfaz de puerto para la gestiÃ³n de auditorÃ­as transaccionales.
 * <p>
 * Define las operaciones necesarias para persistir y consultar los cambios
 * de estado en las entidades de negocio del ecosistema.
 * </p>
 * 
 * @since 2026-05
 */
public interface ServicioAuditoriaTransaccional {

    /**
     * Persiste un nuevo evento de cambio transaccional.
     * 
     * @param evento Datos del cambio (valor anterior, nuevo, entidad, etc.).
     * @return El DTO del evento persistido.
     */
    EventoTransaccionalDTO guardarEvento(EventoTransaccionalDTO evento);

    /**
     * Obtiene el historial de cambios de un usuario especÃ­fico de forma paginada.
     * 
     * @param usuarioId ID del usuario.
     * @param pageable ConfiguraciÃ³n de paginaciÃ³n.
     * @return PÃ¡gina de eventos transaccionales.
     */
    Page<EventoTransaccionalDTO> listarPorUsuario(UUID usuarioId, Pageable pageable);

    /**
     * Filtra eventos transaccionales por microservicio y rango de fechas.
     * 
     * @param servicio Nombre del servicio de origen.
     * @param desde Fecha inicial.
     * @param hasta Fecha final.
     * @param pageable ConfiguraciÃ³n de paginaciÃ³n.
     * @return PÃ¡gina de eventos filtrados.
     */
    Page<EventoTransaccionalDTO> buscarConFiltros(String servicio, LocalDateTime desde, LocalDateTime hasta, Pageable pageable);

    /**
     * Obtiene el Ãºltimo plan registrado para el usuario en sus auditorÃ­as de pago.
     * Si no existen registros anteriores, retorna "FREE" por defecto.
     * 
     * @param usuarioId Identificador del usuario.
     * @return El nombre del Ãºltimo plan financiero registrado.
     */
    String obtenerUltimoPlanUsuario(UUID usuarioId);
}
