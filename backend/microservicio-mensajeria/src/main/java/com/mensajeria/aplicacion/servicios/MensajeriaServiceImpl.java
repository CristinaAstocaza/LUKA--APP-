package com.mensajeria.aplicacion.servicios;

import com.libreria.comun.enums.PropositoCodigo;
import com.libreria.comun.enums.TipoVerificacion;
import com.libreria.comun.respuesta.ResultadoApi;
import com.mensajeria.aplicacion.dtos.solicitudes.SolicitudGenerarCodigo;
import com.mensajeria.aplicacion.dtos.solicitudes.SolicitudValidarCodigo;
import com.mensajeria.aplicacion.dtos.respuestas.RespuestaGeneracion;
import com.mensajeria.aplicacion.dtos.respuestas.RespuestaValidacion;
import com.mensajeria.aplicacion.excepciones.*;
import com.mensajeria.dominio.entidades.CodigoVerificacion;
import com.mensajeria.dominio.entidades.IntentoValidacion;
import com.mensajeria.dominio.especificaciones.MensajeriaSpecs;
import com.mensajeria.dominio.repositorios.CodigoVerificacionRepository;
import com.mensajeria.dominio.repositorios.IntentoValidacionRepository;
import com.mensajeria.aplicacion.puertos.IMensajeriaService;
import com.mensajeria.aplicacion.servicios.canales.NotificacionService;
import com.mensajeria.aplicacion.servicios.canales.TipoNotificacion;
import java.util.Map;
import com.mensajeria.infraestructura.clientes.ClienteUsuario;
import com.mensajeria.infraestructura.clientes.ClienteActualizarTelefono;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

/**
 * ImplementaciÃ³n principal del servicio de mensajerÃ­a y OTP de LUKA APP.
 * <p>
 * Orquesta la generaciÃ³n y validaciÃ³n de cÃ³digos de un solo uso, diferenciando
 * los flujos de {@code ACTIVACION_CUENTA} y {@code RESTABLECER_PASSWORD}.
 * Integra throttling por canal (Redis), auditorÃ­a (RabbitMQ) y sincronizaciÃ³n
 * con el ms-usuario mediante Feign con fallback de Resilience4j.
 * </p>
 *
 * @version 1.1.0
 */
@Service("mensajeriaServiceImpl")
@Slf4j
@RequiredArgsConstructor
public class MensajeriaServiceImpl implements IMensajeriaService {

    private final CodigoVerificacionRepository codigoRepository;
    private final IntentoValidacionRepository intentoRepository;
    private final NotificacionService notificacionService;
    private final ClienteUsuario clienteUsuario;
    private final ClienteActualizarTelefono usuarioFeignClient;

    private static final SecureRandom RANDOM = new SecureRandom();

    private final com.mensajeria.infraestructura.configuracion.PropiedadesOtp propiedadesOtp;
    private final java.util.List<com.mensajeria.aplicacion.servicios.validadores.ValidadorOtp> validadores;
    private final com.mensajeria.aplicacion.fabricas.FabricaCodigoVerificacion fabricaCodigo;
    private final com.mensajeria.infraestructura.configuracion.PropiedadesTwilio propiedadesTwilio;

    // =========================================================================
    // 1. GENERACIÃ“N Y ENVÃO
    // =========================================================================

    /**
     * {@inheritDoc}
     * <p>
     * Flujo interno:
     * <ol>
     *   <li>Valida que el usuario no estÃ© bloqueado por intentos fallidos.</li>
     *   <li>Valida que no haya superado el lÃ­mite diario de 3 cÃ³digos.</li>
     *   <li>Registra el intento de throttling por canal en Redis.</li>
     *   <li>Genera un cÃ³digo OTP aleatorio de 6 dÃ­gitos y lo persiste.</li>
     *   <li>Despacha el cÃ³digo por EMAIL o SMS segÃºn el canal de la solicitud.</li>
     *   <li>Publica un evento de auditorÃ­a asÃ­ncrono vÃ­a RabbitMQ.</li>
     * </ol>
     * </p>
     */
    @Override
    @Transactional
    public RespuestaGeneracion generarYEnviarCodigo(SolicitudGenerarCodigo solicitud) {
        // Ejecutar cadena de responsabilidad para validaciones
        validadores.forEach(v -> v.validar(solicitud));

        String codigo = String.valueOf(100_000 + RANDOM.nextInt(900_000));

        CodigoVerificacion entidad = fabricaCodigo.crear(solicitud, codigo);
        codigoRepository.save(entidad);

        Map<String, Object> variables = Map.of(
                "codigo", codigo,
                "proposito", solicitud.proposito()
        );

        // Resolvemos el canal y el destino
        TipoNotificacion tipoEnvio = switch (solicitud.tipo()) {
            case EMAIL -> TipoNotificacion.EMAIL;
            case SMS -> TipoNotificacion.SMS;
            case WHATSAPP -> TipoNotificacion.WHATSAPP;
        };

        String destino = (solicitud.tipo() == TipoVerificacion.EMAIL) 
                ? solicitud.email() 
                : solicitud.telefono();

        // Enviamos de forma agnÃ³stica (la implementaciÃ³n decide si es SMTP o Twilio)
        notificacionService.enviar(tipoEnvio, destino, variables);



        return new RespuestaGeneracion(true, "CÃ³digo enviado exitosamente", solicitud.tipo());
    }

    // =========================================================================
    // 2. VALIDACIÃ“N â€” ACTIVACIÃ“N DE CUENTA
    // =========================================================================

    /**
     * {@inheritDoc}
     * <p>
     * Si el OTP es correcto, notifica al ms-usuario para activar la cuenta y
     * sincroniza el telÃ©fono si el canal fue SMS.
     * </p>
     */
    @Override
    @Transactional(noRollbackFor = {CodigoInvalidoException.class, CodigoExpiradoException.class, UsuarioBloqueadoExcepcion.class})
    public RespuestaValidacion validarParaActivacion(SolicitudValidarCodigo solicitud) {
        CodigoVerificacion cv = procesarValidacionInterna(solicitud, PropositoCodigo.ACTIVACION_CUENTA);
        String telefonoVerificado = cv.getTelefono();

        log.info("[MS-MENSAJERIA] Activando cuenta para usuario: {} con telÃ©fono: {}",
                cv.getUsuarioId(), telefonoVerificado);

        com.libreria.comun.respuesta.ResultadoApi<String> resultado = clienteUsuario.activarCuenta(cv.getUsuarioId(), telefonoVerificado);

        if (resultado == null || "ACTIVACION_PENDIENTE".equals(resultado.datos())) {
            log.error("[MS-MENSAJERIA] ActivaciÃ³n fallida en ms-usuario para: {}. El OTP sigue siendo vÃ¡lido.", cv.getUsuarioId());
            throw new MensajeriaExternaException("No se pudo activar la cuenta en el servicio de usuario. El OTP sigue activo. Intente de nuevo.", "ms-usuario offline durante la activaciÃ³n");
        }

        // Mover el cv.setUsado(true) a despuÃ©s de que activarCuenta() confirme Ã©xito â€” no antes.
        cv.setUsado(true);
        cv.setFechaUso(LocalDateTime.now());
        reiniciarIntentos(cv.getUsuarioId());
        codigoRepository.save(cv);

        return new RespuestaValidacion(true, "OTP vÃ¡lido. Cuenta activada y telÃ©fono sincronizado.");
    }

    // =========================================================================
    // 3. VALIDACIÃ“N â€” RECUPERACIÃ“N DE CONTRASEÃ‘A
    // =========================================================================

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(noRollbackFor = {CodigoInvalidoException.class, CodigoExpiradoException.class, UsuarioBloqueadoExcepcion.class})
    public UUID validarCodigoYObtenerUsuario(UUID usuarioId, String codigoStr) {
        CodigoVerificacion cv = procesarValidacionInterna(
                new SolicitudValidarCodigo(usuarioId, codigoStr),
                PropositoCodigo.RESTABLECER_PASSWORD);

        cv.setUsado(true);
        cv.setFechaUso(LocalDateTime.now());
        reiniciarIntentos(cv.getUsuarioId());
        codigoRepository.save(cv);

        // Sincronizar el telÃ©fono verificado tras la validaciÃ³n exitosa del OTP
        if (cv.getTipo() == TipoVerificacion.SMS || cv.getTipo() == TipoVerificacion.WHATSAPP) {
            String telefonoVerificado = cv.getTelefono();
            if (telefonoVerificado != null && !telefonoVerificado.isBlank()) {
                try {
                    log.info("[MS-MENSAJERIA] Sincronizando telÃ©fono verificado tras validaciÃ³n OTP exitosa.");
                    ResultadoApi<String> resultado = usuarioFeignClient.sincronizarTelefono(
                            cv.getUsuarioId(), telefonoVerificado);
                    if (resultado != null && "SINCRONIZACION_PENDIENTE".equals(resultado.datos())) {
                        log.warn("[FEIGN] SincronizaciÃ³n de telÃ©fono pendiente en ms-usuario para: {}",
                                cv.getUsuarioId());
                    }
                } catch (Exception e) {
                    log.error("[FEIGN] Error al sincronizar el telÃ©fono con ms-usuario. Fallback no disponible o error interno. Se continuarÃ¡ con el flujo.", e);
                }
            }
        }

        return cv.getUsuarioId();
    }

    // =========================================================================
    // 4. VERIFICACIÃ“N ANTICIPADA DE RESTRICCIONES
    // =========================================================================

    /**
     * {@inheritDoc}
     */
    @Override
    public void verificarRestricciones(UUID usuarioId, PropositoCodigo proposito) {
        verificarBloqueo(usuarioId);
        verificarLimiteDiario(usuarioId, proposito);
        log.info("[MS-MENSAJERIA] Restricciones OK para usuario: {}", usuarioId);
    }

    // =========================================================================
    // LÃ“GICA PRIVADA COMPARTIDA
    // =========================================================================

    /**
     * Valida el OTP internamente para el propÃ³sito dado, registrando intentos
     * fallidos y bloqueando al usuario si supera el mÃ¡ximo configurado.
     *
     * @param sol  DTO con el ID del usuario y el cÃ³digo ingresado.
     * @param prop PropÃ³sito esperado del OTP ({@code ACTIVACION_CUENTA} o
     *             {@code RECUPERACION_PASSWORD}).
     * @return Entidad {@link CodigoVerificacion} validada (no guardada como usada).
     * @throws UsuarioBloqueadoExcepcion si el usuario ya estÃ¡ bloqueado.
     * @throws CodigoPendienteNotFoundException si no hay cÃ³digos pendientes.
     * @throws CodigoExpiradoException si el cÃ³digo ya expirÃ³.
     * @throws CodigoInvalidoException si el cÃ³digo es incorrecto.
     */
    private CodigoVerificacion procesarValidacionInterna(SolicitudValidarCodigo sol, PropositoCodigo prop) {
        verificarBloqueo(sol.usuarioId());

        CodigoVerificacion cv = codigoRepository
                .findTopByUsuarioIdAndPropositoAndUsadoFalseOrderByFechaCreacionDesc(sol.usuarioId(), prop)
                .orElseThrow(() -> new CodigoPendienteNotFoundException(sol.usuarioId()));

        if (cv.isExpirado()) {
            if (registrarIntentoFallido(sol.usuarioId())) {
                throw new UsuarioBloqueadoExcepcion(sol.usuarioId(), propiedadesOtp.getBloqueoHoras());
            }
            throw new CodigoExpiradoException();
        }

        if (!cv.getCodigo().equals(sol.codigo())) {
            if (registrarIntentoFallido(sol.usuarioId())) {
                throw new UsuarioBloqueadoExcepcion(sol.usuarioId(), propiedadesOtp.getBloqueoHoras());
            }
            throw new CodigoInvalidoException("cÃ³digo incorrecto");
        }

        return cv;
    }

    /**
     * Verifica si el usuario tiene un bloqueo activo por intentos fallidos previos.
     *
     * @param uId UUID del usuario a verificar.
     * @throws UsuarioBloqueadoExcepcion si el bloqueo aÃºn no ha expirado.
     */
    private void verificarBloqueo(UUID uId) {
        intentoRepository.findByUsuarioId(uId).ifPresent(i -> {
            if (i.isBloqueado() && !i.bloqueoExpirado()) {
                throw new UsuarioBloqueadoExcepcion(uId,
                        ChronoUnit.HOURS.between(LocalDateTime.now(), i.getBloqueadoHasta()));
            }
        });
    }

    /**
     * Incrementa el contador de intentos fallidos y bloquea al usuario si supera
     * el mÃ¡ximo. Emite advertencia de auditorÃ­a en el segundo intento.
     *
     * @param uId UUID del usuario que fallÃ³ la validaciÃ³n.
     * @return {@code true} si el usuario quedÃ³ bloqueado tras este intento.
     */
    private boolean registrarIntentoFallido(UUID uId) {
        IntentoValidacion i = intentoRepository.findByUsuarioId(uId)
                .orElseGet(() -> IntentoValidacion.builder().usuarioId(uId).build());

        i.incrementarIntentos();
        int intentosActuales = i.getIntentos();

        if (intentosActuales >= propiedadesOtp.getMaxIntentos()) {
            i.bloquear(propiedadesOtp.getBloqueoHoras());
        }

        intentoRepository.save(i);
        return i.isBloqueado();
    }

    /**
     * Reinicia el contador de intentos fallidos del usuario tras una validaciÃ³n
     * exitosa, eliminando cualquier bloqueo activo.
     *
     * @param uId UUID del usuario cuyo registro de intentos debe reiniciarse.
     */
    @org.springframework.cache.annotation.CacheEvict(value="bloqueos-otp", key="#uId")
    public void reiniciarIntentos(UUID uId) {
        intentoRepository.findByUsuarioId(uId).ifPresent(i -> {
            i.reiniciar();
            intentoRepository.save(i);
        });
    }

    /**
     * Verifica que el usuario no haya superado el lÃ­mite de 3 solicitudes diarias
     * para el propÃ³sito dado, contando desde el inicio del dÃ­a actual.
     *
     * @param uId       UUID del usuario a verificar.
     * @param proposito PropÃ³sito del OTP para contar solo las solicitudes del mismo
     *                  tipo.
     * @throws LimiteCodigosExcedidoException si ya agotÃ³ los 3 intentos diarios.
     */
    private void verificarLimiteDiario(UUID uId, PropositoCodigo proposito) {
        LocalDateTime inicioDia = LocalDateTime.now().toLocalDate().atStartOfDay();
        long pedidosHoy = codigoRepository.countByUsuarioIdAndPropositoAndFechaCreacionAfter(
                uId, proposito, inicioDia);

        if (pedidosHoy >= 3) {
            log.warn("[MS-MENSAJERIA] LÃ­mite diario alcanzado â€” usuario: {}, propÃ³sito: {}", uId, proposito);
            throw new LimiteCodigosExcedidoException(
                    "Has alcanzado el lÃ­mite de 3 solicitudes diarias para este trÃ¡mite. IntÃ©ntalo maÃ±ana.");
        }
    }

    // =========================================================================
    // 5. BÃšSQUEDA DINÃMICA â€” SPECIFICATION PATTERN (AuditorÃ­a)
    // =========================================================================

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(readOnly = true)
    public Page<CodigoVerificacion> buscarCodigos(UUID usuarioId, PropositoCodigo proposito,
            Boolean usado, LocalDateTime inicio,
            LocalDateTime fin, Pageable pageable) {
        Specification<CodigoVerificacion> spec = Specification.where(MensajeriaSpecs.porUsuario(usuarioId))
                .and(MensajeriaSpecs.porProposito(proposito))
                .and(MensajeriaSpecs.estaUsado(usado))
                .and(MensajeriaSpecs.creadoEntre(inicio, fin));

        return codigoRepository.findAll(spec, pageable);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean validarConexionTwilio() {
        try {
            // ValidaciÃ³n puramente local (no realiza llamadas a la API de Twilio)
            // Evita errores de permisos (ej. falta de twilio/iam/accounts/read en API Keys Restringidas)
            // y no bloquea el arranque con peticiones de red pesadas en el Health Check.
            String accountSid = propiedadesTwilio.getAccountSid() != null ? propiedadesTwilio.getAccountSid() : propiedadesTwilio.getAccount().getSid();
            String apiKeySid = propiedadesTwilio.getApiKeySid() != null ? propiedadesTwilio.getApiKeySid() : propiedadesTwilio.getApiKey().getSid();
            
            if (accountSid == null || accountSid.isBlank()) {
                throw new IllegalStateException("El Account SID de Twilio no estÃ¡ configurado.");
            }
            
            boolean tieneApiKey = apiKeySid != null && !apiKeySid.isBlank();
            boolean tieneAuthToken = propiedadesTwilio.getAuth().getToken() != null && !propiedadesTwilio.getAuth().getToken().isBlank();

            if (!tieneApiKey && !tieneAuthToken) {
                throw new IllegalStateException("Falta configurar credenciales de Twilio (API Key o Auth Token).");
            }
            
            // log.trace("[TWILIO-HEALTH] ValidaciÃ³n local exitosa. Credenciales presentes para Account SID: {}", accountSid);
            return true;
        } catch (Exception e) {
            log.error("[TWILIO-HEALTH] Error en validaciÃ³n local de Twilio: {}", e.getMessage());
            throw new RuntimeException("Fallo de configuraciÃ³n local en Twilio: " + e.getMessage(), e);
        }
    }
}
