package com.auditoria.presentacion.controladores;

import com.auditoria.aplicacion.puertos.ServicioAuditoriaTransaccional;
import com.libreria.comun.dtos.EventoTransaccionalDTO;
import com.libreria.comun.respuesta.Paginacion;
import com.libreria.comun.respuesta.ResultadoApi;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Controlador para la consulta de auditorÃ­as de cambios transaccionales.
 * <p>
 * Proporciona visibilidad sobre la evoluciÃ³n de los datos en el sistema,
 * permitiendo auditorÃ­as tÃ©cnicas y de negocio.
 * </p>
 * 
 */
@RestController
@RequestMapping("/api/v1/auditoria/transacciones")
@RequiredArgsConstructor
public class AuditoriaTransaccionalControlador {

    private final ServicioAuditoriaTransaccional servicio;

    /**
     * Obtiene el historial de cambios realizados por un usuario especÃ­fico.
     * 
     * @param usuarioId UUID del usuario.
     * @param pagina Ãndice de pÃ¡gina.
     * @param tamanio TamaÃ±o de pÃ¡gina.
     * @return {@link ResultadoApi} con lista paginada de eventos.
     */
    @GetMapping("/usuario/{usuarioId}")
    public ResponseEntity<ResultadoApi<List<EventoTransaccionalDTO>>> listarPorUsuario(
            @PathVariable UUID usuarioId,
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "20") int tamanio) {

        Page<EventoTransaccionalDTO> resultado = servicio.listarPorUsuario(
                usuarioId, PageRequest.of(pagina, Math.min(tamanio, 100)));

        Paginacion<EventoTransaccionalDTO> paginacion = Paginacion.desde(resultado);

        return ResponseEntity.ok(ResultadoApi.exito(
                paginacion.contenido(), 
                "Historial transaccional del usuario recuperado.", 
                paginacion)
        );
    }

    /**
     * BÃºsqueda avanzada de auditorÃ­a transaccional con filtros de servicio y fechas.
     * 
     * @param servicioOrigen Nombre del microservicio.
     * @param desde Fecha de inicio de bÃºsqueda.
     * @param hasta Fecha de fin de bÃºsqueda.
     * @param pagina Ãndice de pÃ¡gina.
     * @return {@link ResultadoApi} con los resultados filtrados.
     */
    @GetMapping("/busqueda")
    public ResponseEntity<ResultadoApi<List<EventoTransaccionalDTO>>> buscar(
            @RequestParam(required = false) String servicioOrigen,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime hasta,
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "20") int tamanio) {

        int tamanioSeguro = Math.min(tamanio, 100);
        Page<EventoTransaccionalDTO> resultado = servicio.buscarConFiltros(
                servicioOrigen, desde, hasta, PageRequest.of(pagina, tamanioSeguro));

        Paginacion<EventoTransaccionalDTO> paginacion = Paginacion.desde(resultado);

        return ResponseEntity.ok(ResultadoApi.exito(
                paginacion.contenido(), 
                "BÃºsqueda transaccional finalizada con Ã©xito.", 
                paginacion)
        );
    }
}

