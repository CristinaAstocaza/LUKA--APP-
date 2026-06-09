package com.auditoria.presentacion.controladores;

import com.auditoria.aplicacion.dtos.RespuestaAuditoriaDetalladoDTO;
import com.auditoria.aplicacion.puertos.ServicioRegistroAuditoria;
import com.libreria.comun.respuesta.Paginacion;
import com.libreria.comun.respuesta.ResultadoApi;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controlador de infraestructura para la gestiÃ³n y consulta de auditorÃ­a.
 * <p>
 * Implementa un modelo hÃ­brido: utiliza contratos de la librerÃ­a comÃºn para la
 * ingesta de datos y DTOs locales detallados para la visualizaciÃ³n
 * administrativa.
 * </p>
 * 
 * @version 1.2
 * @since 2026-05
 */
@RestController
@RequestMapping("/api/v1/auditoria")
@RequiredArgsConstructor
public class AuditoriaControlador {

        private final ServicioRegistroAuditoria servicioAuditoria;

        /**
         * Consulta el histÃ³rico detallado de auditorÃ­a para el Frontend.
         * <p>
         * Retorna un {@link RespuestaAuditoriaDetalladoDTO} que incluye informaciÃ³n
         * contextual (email, nombres) necesaria para la toma de decisiones.
         * </p>
         * 
         * @param modulo  (Opcional) Filtrar por nombre del microservicio.
         * @param pagina  NÃºmero de pÃ¡gina solicitado.
         * @param tamanio Cantidad de registros por pÃ¡gina.
         * @return Respuesta estandarizada con datos detallados y paginaciÃ³n.
         */
        @GetMapping("/registros")
        public ResponseEntity<ResultadoApi<List<RespuestaAuditoriaDetalladoDTO>>> listarRegistros(
                        @RequestParam(required = false) String modulo,
                        @RequestParam(defaultValue = "0") int pagina,
                        @RequestParam(defaultValue = "20") int tamanio) {

                int paginaSegura = Math.max(0, pagina);
                int tamanioSeguro = Math.min(tamanio, 100);

                Pageable paginacionRequest = PageRequest.of(paginaSegura, tamanioSeguro);

                // El servicio ahora debe devolver una pÃ¡gina del DTO detallado local
                Page<RespuestaAuditoriaDetalladoDTO> resultadoPage = servicioAuditoria.listarRegistrosDetallados(modulo,
                                paginacionRequest);

                Paginacion<RespuestaAuditoriaDetalladoDTO> infoPaginacion = Paginacion.desde(resultadoPage);

                return ResponseEntity.ok(
                                ResultadoApi.exito(
                                                infoPaginacion.contenido(),
                                                "Consulta de registros detallada realizada con Ã©xito.",
                                                infoPaginacion));
        }
}
