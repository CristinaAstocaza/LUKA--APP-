package com.auditoria.presentacion.controladores;

import com.auditoria.aplicacion.puertos.ServicioAuditoriaAcceso;
import com.libreria.comun.dtos.EventoAccesoDTO;
import com.libreria.comun.respuesta.Paginacion;
import com.libreria.comun.respuesta.ResultadoApi;

import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Controlador de presentaciÃ³n para la gestiÃ³n de auditorÃ­as de acceso.
 * <p>
 * Expone endpoints para la consulta de registros de inicio de sesiÃ³n y actividad,
 * integrando el estÃ¡ndar de respuestas {@link ResultadoApi} y metadatos de {@link Paginacion}.
 * </p>
 * 
 * @version 1.3
 * @since 2026-05
 */
@RestController
@RequestMapping("/api/v1/auditoria/accesos")
@RequiredArgsConstructor
public class AuditoriaAccesoControlador {

    private final ServicioAuditoriaAcceso servicio;

    /**
     * Recupera la lista paginada de todos los eventos de acceso registrados en el sistema.
     * 
     * @param pagina  NÃºmero de pÃ¡gina (0 por defecto).
     * @param tamanio Cantidad de registros (20 por defecto, mÃ¡x 100).
     * @return {@link ResponseEntity} con el {@link ResultadoApi} y metadatos de paginaciÃ³n.
     */
    @GetMapping
    public ResponseEntity<ResultadoApi<List<EventoAccesoDTO>>> listarTodo(
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "20") int tamanio) {

        int tamanioSeguro = Math.min(tamanio, 100);
        Page<EventoAccesoDTO> resultadoPage = servicio.listarTodo(PageRequest.of(pagina, tamanioSeguro));
        
        Paginacion<EventoAccesoDTO> metadata = Paginacion.desde(resultadoPage);

        return ResponseEntity.ok(
            ResultadoApi.exito(
                metadata.contenido(), 
                "CatÃ¡logo de accesos recuperado exitosamente.", 
                metadata
            )
        );
    }

    /**
     * Busca los registros de acceso asociados a un usuario especÃ­fico.
     * 
     * @param usuarioId Identificador Ãºnico del usuario (UUID).
     * @param pagina    NÃºmero de pÃ¡gina solicitado.
     * @return {@link ResponseEntity} con el resultado paginado para el usuario indicado.
     */
    @GetMapping("/usuario/{usuarioId}")
    public ResponseEntity<ResultadoApi<List<EventoAccesoDTO>>> obtenerPorUsuario(
            @PathVariable UUID usuarioId,
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "20") int tamanio) {

        int tamanioSeguro = Math.min(tamanio, 100);
        Page<EventoAccesoDTO> resultadoPage = servicio.listarPorUsuario(usuarioId, PageRequest.of(pagina, tamanioSeguro));
        
        Paginacion<EventoAccesoDTO> metadata = Paginacion.desde(resultadoPage);
        String mensaje = String.format("Registros de acceso para el usuario %s recuperados.", usuarioId);

        return ResponseEntity.ok(
            ResultadoApi.exito(metadata.contenido(), mensaje, metadata)
        );
    }
}
