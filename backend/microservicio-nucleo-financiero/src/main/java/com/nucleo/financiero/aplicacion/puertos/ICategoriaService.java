package com.nucleo.financiero.aplicacion.puertos;

import com.nucleo.financiero.aplicacion.dtos.respuestas.CategoriaDTO;
import com.nucleo.financiero.aplicacion.dtos.solicitudes.CategoriaRequestDTO;
import com.nucleo.financiero.dominio.entidades.Categoria.TipoMovimiento;
import java.util.List;
import java.util.UUID;

/**
 * Interfaz de servicio para la gestiÃ³n de categorÃ­as financieras.
 * Define el contrato de negocio para el registro y consulta de categorÃ­as.
 *
 * @version 1.2.0
 */
public interface ICategoriaService {

    /**
     * Registra una nueva categorÃ­a en el sistema.
     * @param request Datos de la categorÃ­a a crear
     * @return DTO de la categorÃ­a creada
     * @throws IllegalStateException Si el nombre de la categorÃ­a ya existe
     */
    CategoriaDTO crear(CategoriaRequestDTO request);

    /**
     * Lista todas las categorÃ­as registradas.
     * @return Lista de DTOs de categorÃ­as
     */
    List<CategoriaDTO> listarTodas();

    /**
     * Filtra categorÃ­as por tipo de movimiento (INGRESO/EGRESO).
     * @param tipo Tipo de movimiento
     * @return Lista filtrada de DTOs
     */
    List<CategoriaDTO> listarPorTipo(TipoMovimiento tipo);

    /**
     * Obtiene el detalle de una categorÃ­a por su ID.
     * @param id Identificador Ãºnico de la categorÃ­a
     * @return DTO de la categorÃ­a
     * @throws IllegalArgumentException Si la categorÃ­a no existe
     */
    CategoriaDTO obtenerPorId(UUID id);

    /**
     * Actualiza los datos de una categorÃ­a existente.
     * @param id Identificador de la categorÃ­a
     * @param request Nuevos datos
     * @return DTO actualizado
     */
    CategoriaDTO actualizar(UUID id, CategoriaRequestDTO request);

    /**
     * Elimina una categorÃ­a del sistema.
     * @param id Identificador de la categorÃ­a
     */
    void eliminar(UUID id);
}
