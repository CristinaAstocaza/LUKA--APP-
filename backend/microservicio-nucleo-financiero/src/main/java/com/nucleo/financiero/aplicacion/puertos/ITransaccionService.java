package com.nucleo.financiero.aplicacion.puertos;

import com.nucleo.financiero.aplicacion.dtos.respuestas.ResumenFinancieroDTO;
import com.nucleo.financiero.aplicacion.dtos.respuestas.RespuestaTransaccion;
import com.nucleo.financiero.aplicacion.dtos.solicitudes.SolicitudTransaccion;
import com.nucleo.financiero.dominio.entidades.Categoria.TipoMovimiento;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Interfaz de servicio para la gestiÃ³n de transacciones financieras.
 * <p>
 * Define el contrato para el registro, consulta y anÃ¡lisis de movimientos
 * financieros (Ingresos y Egresos).
 * </p>
 *
 * @version 1.2.0
 */
public interface ITransaccionService {

    /**
     * Registra una nueva transacciÃ³n individual.
     *
     * @param request Datos de la transacciÃ³n a registrar.
     * @param ipCliente DirecciÃ³n IP de origen para auditorÃ­a.
     * @return DTO con los datos de la transacciÃ³n persistida.
     */
    RespuestaTransaccion registrar(SolicitudTransaccion request, String ipCliente);

    /**
     * Registra un lote de transacciones en una sola operaciÃ³n atÃ³mica.
     *
     * @param solicitudes Lista de solicitudes de transacciÃ³n.
     * @param ipCliente DirecciÃ³n IP de origen.
     * @return Lista de DTOs de las transacciones registradas.
     */
    List<RespuestaTransaccion> registrarLote(List<SolicitudTransaccion> solicitudes, String ipCliente);

    /**
     * Consulta el historial de transacciones de un usuario con filtros.
     *
     * @param usuarioId ID del usuario.
     * @param tipo Filtro por tipo de movimiento (opcional).
     * @param categoriaId Filtro por categorÃ­a (opcional).
     * @param desde Fecha de inicio del rango (opcional).
     * @param hasta Fecha de fin del rango (opcional).
     * @param paginacion ParÃ¡metros de paginaciÃ³n.
     * @param ipCliente DirecciÃ³n IP de origen.
     * @return PÃ¡gina de resultados de transacciones.
     */
    Page<RespuestaTransaccion> listarHistorial(
            UUID usuarioId, TipoMovimiento tipo, UUID categoriaId,
            LocalDateTime desde, LocalDateTime hasta, Pageable paginacion,
            String ipCliente);

    /**
     * Obtiene un resumen financiero consolidado por periodo.
     *
     * @param usuarioId ID del usuario.
     * @param mes Mes del resumen (1-12).
     * @param anio AÃ±o del resumen.
     * @param ipCliente DirecciÃ³n IP de origen.
     * @return DTO con el resumen de ingresos, gastos y contadores.
     */
    ResumenFinancieroDTO obtenerResumen(UUID usuarioId, Integer mes, Integer anio, String ipCliente);

    /**
     * Busca una transacciÃ³n por su identificador Ãºnico.
     *
     * @param id UUID de la transacciÃ³n.
     * @return DTO de la transacciÃ³n encontrada.
     * @throws IllegalArgumentException Si la transacciÃ³n no existe.
     */
    RespuestaTransaccion obtenerPorId(UUID id);
}
