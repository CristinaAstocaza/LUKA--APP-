package com.mensajeria.presentacion.controladores;

import com.mensajeria.aplicacion.dtos.solicitudes.*;
import com.mensajeria.aplicacion.dtos.respuestas.RespuestaGeneracion;
import com.mensajeria.aplicacion.dtos.respuestas.RespuestaValidacion;
import com.mensajeria.aplicacion.puertos.IMensajeriaService;
import com.mensajeria.dominio.entidades.CodigoVerificacion;
import com.libreria.comun.enums.PropositoCodigo;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.libreria.comun.respuesta.ResultadoApi;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Controlador REST para la gestiÃ³n de OTPs y validaciones de mensajerÃ­a.
 * <p>
 * Expone los endpoints pÃºblicos que los microservicios cliente (ms-usuario) y
 * la app frontend consumen para los flujos de activaciÃ³n de cuenta y
 * recuperaciÃ³n de contraseÃ±a. La inyecciÃ³n se hace exclusivamente a travÃ©s de
 * la interfaz {@link IMensajeriaService}, respetando el principio de inversiÃ³n
 * de dependencias (SOLID-D).
 * </p>
 *
 * @version 1.2.0
 */
@RestController
@RequestMapping("/api/v1/mensajeria/otp")
@RequiredArgsConstructor
@Slf4j
public class ControladorMensajeria {

    /** Servicio inyectado por interfaz â€” Spring resuelve {@code MensajeriaServiceImpl}. */
    private final IMensajeriaService mensajeriaService;

    /**
     * Genera y envÃ­a un cÃ³digo OTP al canal elegido por el usuario.
     * <p>
     * Punto de entrada Ãºnico para los flujos de activaciÃ³n de cuenta y
     * recuperaciÃ³n de contraseÃ±a. Aplica bloqueo, lÃ­mite diario y throttling
     * antes de persistir el cÃ³digo.
     * </p>
     *
     * @param solicitud DTO con {@code usuarioId}, {@code email}, {@code telefono},
     *                  {@code tipo} (EMAIL | SMS) y {@code proposito}
     *                  (ACTIVACION_CUENTA | RESTABLECER_PASSWORD).
     * @return HTTP 201 con {@link ResultadoApi} envolviendo {@link RespuestaGeneracion}.
     */
    @PostMapping("/generar")
    public ResponseEntity<ResultadoApi<RespuestaGeneracion>> generarCodigo(
            @Valid @RequestBody SolicitudGenerarCodigo solicitud) {

        log.debug("[POST] /otp/generar â€” usuarioId: {}, propÃ³sito: {}",
                solicitud.usuarioId(), solicitud.proposito());

        RespuestaGeneracion respuesta = mensajeriaService.generarYEnviarCodigo(solicitud);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ResultadoApi.creado(respuesta, "CÃ³digo generado exitosamente"));
    }

    /**
     * Valida el OTP en el flujo de activaciÃ³n de cuenta.
     * <p>
     * Si el cÃ³digo es correcto, notifica al ms-usuario para activar la cuenta
     * y sincroniza el telÃ©fono verificado.
     * </p>
     *
     * @param solicitud DTO con {@code usuarioId} y el {@code codigo} OTP ingresado.
     * @return HTTP 200 con {@link ResultadoApi} envolviendo {@link RespuestaValidacion}.
     */
    @PostMapping("/validar-activacion")
    public ResponseEntity<ResultadoApi<RespuestaValidacion>> validarActivacion(
            @Valid @RequestBody SolicitudValidarCodigo solicitud) {

        log.debug("[POST] /otp/validar-activacion â€” usuarioId: {}", solicitud.usuarioId());

        RespuestaValidacion respuesta = mensajeriaService.validarParaActivacion(solicitud);
        return ResponseEntity.ok(ResultadoApi.exito(respuesta, "CÃ³digo validado exitosamente", null));
    }

    /**
     * Valida el OTP para el flujo de recuperaciÃ³n de contraseÃ±a.
     * <p>
     * Consumido internamente por el ms-usuario para obtener el UUID del
     * usuario propietario del cÃ³digo antes de emitir el token de reset.
     * </p>
     *
     * @param solicitud DTO con el usuarioId (registroId) y el codigo OTP.
     * @return HTTP 200 con el UUID del usuario validado, listo para el reset.
     */
    @PostMapping("/validar-recuperacion")
    public ResponseEntity<ResultadoApi<UUID>> validarRecuperacion(
            @Valid @RequestBody SolicitudRecuperacion solicitud) {

        log.debug("[POST] /otp/validar-recuperacion â€” iniciando validaciÃ³n de reset");

        UUID usuarioValidado = mensajeriaService.validarCodigoYObtenerUsuario(solicitud.usuarioId(), solicitud.codigo());
        return ResponseEntity.ok(ResultadoApi.exito(usuarioValidado, "Usuario validado exitosamente", null));
    }

    /**
     * Verifica anticipadamente las restricciones de bloqueo y lÃ­mite diario
     * para el usuario dado, sin generar ni enviar ningÃºn cÃ³digo OTP.
     * <p>
     * Ãštil para que el frontend deshabilite el botÃ³n de reenvÃ­o antes de que
     * el usuario lo intente y reciba un error 429.
     * </p>
     *
     * @param solicitud DTO con {@code usuarioId} y {@code proposito} a verificar.
     * @return HTTP 200 si el usuario puede solicitar un cÃ³digo; error semÃ¡ntico
     *         del {@code ManejadorGlobalExcepciones} si estÃ¡ bloqueado o agotÃ³
     *         su lÃ­mite diario.
     */
    @PostMapping("/validar-limite")
    public ResponseEntity<ResultadoApi<Void>> validarLimite(
            @Valid @RequestBody SolicitudVerificarLimite solicitud) {

        mensajeriaService.verificarRestricciones(solicitud.usuarioId(), solicitud.proposito());
        return ResponseEntity.ok(ResultadoApi.sinContenido("Restricciones verificadas exitosamente"));
    }

    // =========================================================================
    // ENDPOINT ADMINISTRATIVO â€” BÃšSQUEDA DINÃMICA (Specification Pattern)
    // =========================================================================

    /**
     * Busca cÃ³digos de verificaciÃ³n OTP de forma dinÃ¡mica cruzando filtros.
     * <p>
     * Endpoint administrativo protegido por {@code ROLE_ADMIN} (vÃ­a SecurityConfig)
     * que consume el Specification Pattern de {@code MensajeriaSpecs} para auditar
     * el historial de OTPs de cualquier usuario con combinaciones de filtros.
     * </p>
     *
     * @param usuarioId Filtra por ID del usuario (opcional).
     * @param proposito Filtra por propÃ³sito: ACTIVACION_CUENTA o RESTABLECER_PASSWORD (opcional).
     * @param usado     Filtra por estado de uso del OTP (opcional).
     * @param inicio    Fecha inicio del rango de creaciÃ³n (opcional, ISO format).
     * @param fin       Fecha fin del rango de creaciÃ³n (opcional, ISO format).
     * @param pagina    NÃºmero de pÃ¡gina (0-indexed, default 0).
     * @param tamanio   Elementos por pÃ¡gina (default 20).
     * @return HTTP 200 con {@link ResultadoApi} envolviendo la pÃ¡gina de resultados.
     */
    @GetMapping("/buscar")
    public ResponseEntity<ResultadoApi<Page<CodigoVerificacion>>> buscarCodigos(
            @RequestParam(required = false) UUID usuarioId,
            @RequestParam(required = false) PropositoCodigo proposito,
            @RequestParam(required = false) Boolean usado,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime inicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fin,
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "20") int tamanio) {

        log.debug("[GET] /otp/buscar â€” usuarioId: {}, proposito: {}, usado: {}", usuarioId, proposito, usado);

        Page<CodigoVerificacion> resultados = mensajeriaService.buscarCodigos(
                usuarioId, proposito, usado, inicio, fin,
                PageRequest.of(pagina, tamanio, Sort.by(Sort.Direction.DESC, "fechaCreacion")));

        return ResponseEntity.ok(ResultadoApi.exito(resultados, "BÃºsqueda de OTPs completada", null));
    }

    /**
     * Endpoint pÃºblico/interno de salud para validar que las credenciales de Twilio
     * (sea la API Key o el Auth Token Maestro) estÃ¡n configuradas y funcionando con Ã©xito.
     * Realiza un fetch rÃ¡pido y no destructivo a la API de Twilio.
     *
     * @return HTTP 200 si la conexiÃ³n es exitosa, o error detallado en caso contrario.
     */
    @GetMapping("/twilio/health")
    public ResponseEntity<ResultadoApi<Boolean>> verificarSaludTwilio() {
        log.info("[GET] /otp/twilio/health â€” Iniciando prueba de comunicaciÃ³n con Twilio");
        boolean saludOk = mensajeriaService.validarConexionTwilio();
        return ResponseEntity.ok(ResultadoApi.exito(saludOk, "La integraciÃ³n con Twilio estÃ¡ configurada y lista para operar.", null));
    }
}

