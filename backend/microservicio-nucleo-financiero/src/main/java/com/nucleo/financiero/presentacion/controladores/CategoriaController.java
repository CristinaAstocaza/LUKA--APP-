package com.nucleo.financiero.presentacion.controladores;

import com.libreria.comun.respuesta.ResultadoApi;
import com.nucleo.financiero.aplicacion.dtos.respuestas.CategoriaDTO;
import com.nucleo.financiero.aplicacion.dtos.solicitudes.CategoriaRequestDTO;
import com.nucleo.financiero.aplicacion.puertos.ICategoriaService;
import com.nucleo.financiero.dominio.entidades.Categoria.TipoMovimiento;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.UUID;

/**
 * Controlador REST para la gestiÃ³n de categorÃ­as financieras.
 * <p>
 * Estandarizado con {@link ResultadoApi} para proporcionar respuestas consistentes
 * a los consumidores del frontend y otros microservicios.
 * </p>
 *
 * @version 1.2.1
 */
@RestController
@RequestMapping("/api/v1/financiero/categorias")
@RequiredArgsConstructor
@Slf4j
public class CategoriaController {

    private final ICategoriaService categoriaService;

    /**
     * Registra una nueva categorÃ­a en el sistema.
     * 
     * @param request Datos de la categorÃ­a validados.
     * @return ResponseEntity con la categorÃ­a creada.
     */
    @PostMapping
    public ResponseEntity<ResultadoApi<CategoriaDTO>> crear(@Valid @RequestBody CategoriaRequestDTO request) {
        log.info("REST request para crear categorÃ­a: {}", request.nombre());
        CategoriaDTO dto = categoriaService.crear(request);
        return ResponseEntity.status(201).body(ResultadoApi.creado(dto, "CategorÃ­a creada correctamente"));
    }

    /**
     * Lista todas las categorÃ­as, permitiendo filtrado opcional por tipo de movimiento.
     * 
     * @param tipo Tipo de movimiento (INGRESO o EGRESO). Opcional.
     * @return ResponseEntity con la lista de categorÃ­as encontradas.
     */
    @GetMapping
    public ResponseEntity<ResultadoApi<List<CategoriaDTO>>> listar(@RequestParam(required = false) TipoMovimiento tipo) {
        List<CategoriaDTO> lista = (tipo != null) 
                ? categoriaService.listarPorTipo(tipo) 
                : categoriaService.listarTodas();
        // Se utiliza la sobrecarga de exito(T datos) para listas
        return ResponseEntity.ok(ResultadoApi.exito(lista));
    }

    /**
     * Recupera el detalle de una categorÃ­a especÃ­fica por su identificador Ãºnico.
     * 
     * @param id UUID de la categorÃ­a.
     * @return ResponseEntity con el DTO de la categorÃ­a.
     */
    @GetMapping("/{id}")
    public ResponseEntity<ResultadoApi<CategoriaDTO>> obtener(@PathVariable UUID id) {
        CategoriaDTO dto = categoriaService.obtenerPorId(id);
        return ResponseEntity.ok(ResultadoApi.exito(dto));
    }

    /**
     * Actualiza los atributos de una categorÃ­a existente.
     * 
     * @param id UUID de la categorÃ­a a modificar.
     * @param request Nuevos datos de la categorÃ­a.
     * @return ResponseEntity con el estado actualizado.
     */
    @PutMapping("/{id}")
    public ResponseEntity<ResultadoApi<CategoriaDTO>> actualizar(@PathVariable UUID id, @Valid @RequestBody CategoriaRequestDTO request) {
        CategoriaDTO dto = categoriaService.actualizar(id, request);
        return ResponseEntity.ok(ResultadoApi.exito(dto, "CategorÃ­a actualizada correctamente"));
    }

    /**
     * Elimina una categorÃ­a del sistema.
     * 
     * @param id UUID de la categorÃ­a a eliminar.
     * @return ResponseEntity confirmando la eliminaciÃ³n.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<ResultadoApi<Void>> eliminar(@PathVariable UUID id) {
        categoriaService.eliminar(id);
        return ResponseEntity.ok(ResultadoApi.sinContenido("CategorÃ­a eliminada correctamente"));
    }
}
