package com.nucleo.financiero.presentacion.controladores;

import com.libreria.comun.respuesta.Paginacion;
import com.libreria.comun.respuesta.ResultadoApi;
import com.libreria.comun.utilidades.UtilidadIp;
import com.libreria.comun.utilidades.UtilidadSeguridad;
import com.libreria.comun.excepciones.ExcepcionAccesoDenegado;
import com.nucleo.financiero.aplicacion.dtos.solicitudes.SolicitudTransaccion;
import com.nucleo.financiero.aplicacion.dtos.respuestas.RespuestaTransaccion;
import com.nucleo.financiero.aplicacion.dtos.respuestas.ResumenFinancieroDTO;
import com.nucleo.financiero.aplicacion.puertos.ITransaccionService;
import com.nucleo.financiero.dominio.entidades.Categoria.TipoMovimiento;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.UUID;
import org.springframework.format.annotation.DateTimeFormat;

/**
 * Controlador REST para el registro y gestiÃ³n de transacciones financieras.
 * <p>
 * Centraliza la lÃ³gica de ingresos y egresos manuales, permitiendo tanto
 * registros
 * individuales como en lote. Se comunica a travÃ©s del contrato
 * {@link ITransaccionService}.
 * </p>
 *
 * @version 1.2.2
 */
@RestController
@RequestMapping("/api/v1/financiero/transacciones")
@RequiredArgsConstructor
@Slf4j
@Validated
public class TransaccionController {

    private final ITransaccionService transaccionService;

    /**
     * Registra un movimiento financiero individual (Ingreso/Egreso).
     * 
     * @param request     Datos de la transacciÃ³n.
     * @param httpRequest Datos de la peticiÃ³n para auditorÃ­a.
     * @return ResponseEntity con la transacciÃ³n registrada.
     */
    @PostMapping
    public ResponseEntity<ResultadoApi<RespuestaTransaccion>> registrar(
            @Valid @RequestBody SolicitudTransaccion request,
            HttpServletRequest httpRequest) {

        UUID tokenUsuarioId = UtilidadSeguridad.obtenerUsuarioId();
        if (!tokenUsuarioId.equals(request.usuarioId())) {
            throw new ExcepcionAccesoDenegado();
        }

        RespuestaTransaccion respuesta = transaccionService.registrar(request, UtilidadIp.obtenerIpReal(httpRequest));
        return ResponseEntity.status(201).body(ResultadoApi.creado(respuesta, "TransacciÃ³n registrada con Ã©xito"));
    }

    /**
     * Registra un conjunto de transacciones en una sola operaciÃ³n (Lote).
     * <p>
     * Optimizado para importaciones masivas o sincronizaciones iniciales.
     * </p>
     * 
     * @param solicitudes Lista de transacciones a registrar.
     * @param httpRequest Datos de la peticiÃ³n para auditorÃ­a.
     * @return ResponseEntity con el listado de transacciones procesadas.
     */
    @PostMapping("/lote")
    public ResponseEntity<ResultadoApi<List<RespuestaTransaccion>>> registrarLote(
            @Valid @RequestBody List<@Valid SolicitudTransaccion> solicitudes,
            HttpServletRequest httpRequest) {

        UUID tokenUsuarioId = UtilidadSeguridad.obtenerUsuarioId();
        for (SolicitudTransaccion request : solicitudes) {
            if (!tokenUsuarioId.equals(request.usuarioId())) {
                throw new ExcepcionAccesoDenegado();
            }
        }

        List<RespuestaTransaccion> respuesta = transaccionService.registrarLote(solicitudes, UtilidadIp.obtenerIpReal(httpRequest));
        return ResponseEntity.status(201).body(ResultadoApi.creado(respuesta, "Lote de transacciones procesado"));
    }

    /**
     * Consulta el historial paginado de transacciones con filtros dinÃ¡micos.
     * 
     * @param usuarioId   ID del propietario de las transacciones.
     * @param tipo        Filtro por INGRESO o EGRESO.
     * @param categoriaId Filtro por categorÃ­a especÃ­fica.
     * @param desde       Fecha inicial del rango.
     * @param hasta       Fecha final del rango.
     * @param pagina      NÃºmero de pÃ¡gina (0-based).
     * @param tamanio     Elementos por pÃ¡gina.
     * @param httpRequest Datos de la peticiÃ³n.
     * @return ResponseEntity con el resultado paginado estandarizado.
     */
    @GetMapping("/historial")
    public ResponseEntity<ResultadoApi<List<RespuestaTransaccion>>> listarHistorial(
            @RequestParam UUID usuarioId,
            @RequestParam(required = false) TipoMovimiento tipo,
            @RequestParam(required = false) UUID categoriaId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime hasta,
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "20") int tamanio,
            HttpServletRequest httpRequest) {

        UUID tokenUsuarioId = UtilidadSeguridad.obtenerUsuarioId();
        if (!tokenUsuarioId.equals(usuarioId)) {
            throw new ExcepcionAccesoDenegado();
        }

        Pageable paginacionSpring = PageRequest.of(pagina, tamanio, Sort.by("fechaTransaccion").descending());
        Page<RespuestaTransaccion> page = transaccionService.listarHistorial(
                usuarioId, tipo, categoriaId, desde, hasta, paginacionSpring, UtilidadIp.obtenerIpReal(httpRequest));

        return ResponseEntity.ok(ResultadoApi.exito(
                page.getContent(),
                "Historial recuperado",
                Paginacion.desde(page)));
    }

    /**
     * Obtiene un resumen consolidado de las finanzas en un periodo determinado.
     * 
     * @param usuarioId   ID del usuario.
     * @param mes         Mes del resumen (1-12).
     * @param anio        AÃ±o del resumen.
     * @param httpRequest Datos de la peticiÃ³n.
     * @return ResponseEntity con el DTO de resumen financiero.
     */
    @GetMapping("/resumen")
    public ResponseEntity<ResultadoApi<ResumenFinancieroDTO>> obtenerResumen(
            @RequestParam UUID usuarioId,
            @RequestParam(required = false) Integer mes,
            @RequestParam(required = false) Integer anio,
            HttpServletRequest httpRequest) {

        UUID tokenUsuarioId = UtilidadSeguridad.obtenerUsuarioId();
        if (!tokenUsuarioId.equals(usuarioId)) {
            throw new ExcepcionAccesoDenegado();
        }

        ResumenFinancieroDTO resumen = transaccionService.obtenerResumen(usuarioId, mes, anio, UtilidadIp.obtenerIpReal(httpRequest));
        return ResponseEntity.ok(ResultadoApi.exito(resumen, "Resumen financiero generado"));
    }

    /**
     * Busca una transacciÃ³n especÃ­fica por su identificador Ãºnico.
     * 
     * @param id UUID de la transacciÃ³n.
     * @return ResponseEntity con el detalle de la transacciÃ³n.
     */
    @GetMapping("/{id}")
    public ResponseEntity<ResultadoApi<RespuestaTransaccion>> obtenerPorId(@PathVariable UUID id) {
        RespuestaTransaccion respuesta = transaccionService.obtenerPorId(id);
        return ResponseEntity.ok(ResultadoApi.exito(respuesta));
    }

}
