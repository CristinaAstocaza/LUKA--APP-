package com.auditoria.aplicacion.puertos;

import com.libreria.comun.dtos.EventoAccesoDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.UUID;

/**
 * Interfaz de puerto para la gestiÃ³n de auditorÃ­as de acceso.
 * <p>
 * Se encarga exclusivamente de la persistencia, consulta y mantenimiento 
 * de los registros de inicio de sesiÃ³n y actividad de usuarios.
 * </p>
 * 
 * @since 2026-05
 */
public interface ServicioAuditoriaAcceso {

    /**
     * Registra un nuevo intento de acceso (Ã©xito o fallo).
     * 
     * @param dto Datos del intento de acceso.
     * @return El registro persistido en formato DTO.
     */
    EventoAccesoDTO registrarAcceso(EventoAccesoDTO dto);

    /**
     * Recupera una lista paginada de todos los accesos registrados.
     * 
     * @param paginacion ConfiguraciÃ³n de pÃ¡gina y tamaÃ±o.
     * @return PÃ¡gina de registros de acceso.
     */
    Page<EventoAccesoDTO> listarTodo(Pageable paginacion);

    /**
     * Filtra los registros de acceso de un usuario especÃ­fico.
     * 
     * @param usuarioId Identificador Ãºnico del usuario.
     * @param paginacion ConfiguraciÃ³n de paginaciÃ³n.
     * @return PÃ¡gina de registros asociados al usuario.
     */
    Page<EventoAccesoDTO> listarPorUsuario(UUID usuarioId, Pageable paginacion);

    /**
     * Purga registros de acceso que superen la antigÃ¼edad permitida por la polÃ­tica de retenciÃ³n.
     * 
     * @param diasAntiguedad Cantidad de dÃ­as hacia atrÃ¡s para mantener.
     * @return Cantidad de registros eliminados.
     */
    int limpiarRegistrosAntiguos(int diasAntiguedad);
}
