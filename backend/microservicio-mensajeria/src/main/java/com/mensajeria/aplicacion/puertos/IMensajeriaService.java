package com.mensajeria.aplicacion.puertos;

import com.libreria.comun.enums.PropositoCodigo;
import com.mensajeria.aplicacion.dtos.solicitudes.SolicitudGenerarCodigo;
import com.mensajeria.aplicacion.dtos.solicitudes.SolicitudValidarCodigo;
import com.mensajeria.aplicacion.dtos.respuestas.RespuestaGeneracion;
import com.mensajeria.aplicacion.dtos.respuestas.RespuestaValidacion;

/**
 * Contrato del servicio principal de mensajerÃ­a y OTP.
 * <p>
 * Expone las operaciones de generaciÃ³n, validaciÃ³n y restricciÃ³n de cÃ³digos
 * de un solo uso (OTP), diferenciando flujos de activaciÃ³n y recuperaciÃ³n.
 * </p>
 *
 * @version 1.1.0
 */
public interface IMensajeriaService {

    /**
     * Genera un cÃ³digo OTP de 6 dÃ­gitos, lo persiste y lo envÃ­a al canal
     * indicado en la solicitud (EMAIL o SMS). Aplica verificaciÃ³n de bloqueo y
     * lÃ­mite diario antes del envÃ­o.
     *
     * @param solicitud DTO con los datos del usuario (ID, email, telÃ©fono,
     *                  canal y propÃ³sito).
     * @return {@code RespuestaGeneracion} con el estado del envÃ­o y el canal
     *         utilizado.
     * @throws com.mensajeria.aplicacion.excepciones.UsuarioBloqueadoExcepcion
     *             si el usuario estÃ¡ bloqueado por intentos fallidos previos.
     * @throws com.mensajeria.aplicacion.excepciones.LimiteCodigosExcedidoException
     *             si el usuario ya agotÃ³ los 3 cÃ³digos diarios para ese propÃ³sito.
     */
    RespuestaGeneracion generarYEnviarCodigo(SolicitudGenerarCodigo solicitud);

    /**
     * Valida el OTP para el flujo de activaciÃ³n de cuenta. Si es correcto,
     * notifica al ms-usuario para activar la cuenta y sincronizar el telÃ©fono.
     *
     * @param solicitud DTO con el ID del usuario y el cÃ³digo OTP ingresado.
     * @return {@code RespuestaValidacion} confirmando la activaciÃ³n exitosa.
     * @throws com.mensajeria.aplicacion.excepciones.UsuarioBloqueadoExcepcion
     *             si el usuario ya estÃ¡ bloqueado.
     * @throws com.mensajeria.aplicacion.excepciones.CodigoInvalidoException
     *             si el cÃ³digo es incorrecto o ya fue usado.
     */
    RespuestaValidacion validarParaActivacion(SolicitudValidarCodigo solicitud);

    /**
     * Valida el OTP para el flujo de recuperaciÃ³n de contraseÃ±a. Retorna el UUID
     * del usuario asociado al cÃ³digo para que el ms-usuario inicie el reset.
     *
     * @param registroId UUID del registro OTP, enviado por el ms-usuario como
     *                   identificador del proceso de recuperaciÃ³n.
     * @param codigoStr  CÃ³digo OTP de 6 dÃ­gitos ingresado por el usuario.
     * @return UUID del usuario propietario del cÃ³digo, para ser usado por el
     *         ms-usuario al generar el token de reset.
     * @throws IllegalArgumentException si el par (registroId, cÃ³digo) no existe o
     *                                  ya fue usado.
     * @throws IllegalStateException    si el cÃ³digo ya expirÃ³.
     */
    java.util.UUID validarCodigoYObtenerUsuario(java.util.UUID usuarioId, String codigoStr);

    /**
     * Valida de forma anticipada las restricciones de bloqueo y lÃ­mite diario
     * para el usuario dado, sin generar ningÃºn cÃ³digo. Usado por el controlador
     * en el endpoint {@code /validar-limite}.
     *
     * @param usuarioId UUID del usuario a verificar.
     * @param proposito PropÃ³sito del OTP ({@code ACTIVACION_CUENTA} o
     *                  {@code RECUPERACION_PASSWORD}) para verificar el lÃ­mite
     *                  correcto.
     * @throws com.mensajeria.aplicacion.excepciones.UsuarioBloqueadoExcepcion
     *             si el usuario estÃ¡ bloqueado.
     * @throws com.mensajeria.aplicacion.excepciones.LimiteCodigosExcedidoException
     *             si ya superÃ³ el lÃ­mite diario.
     */
    void verificarRestricciones(java.util.UUID usuarioId, PropositoCodigo proposito);

    /**
     * Busca cÃ³digos de verificaciÃ³n dinÃ¡micamente utilizando Specifications.
     * MÃ©todo administrativo para auditorÃ­a de OTPs histÃ³ricos.
     */
    org.springframework.data.domain.Page<com.mensajeria.dominio.entidades.CodigoVerificacion> buscarCodigos(
            java.util.UUID usuarioId,
            PropositoCodigo proposito,
            Boolean usado,
            java.time.LocalDateTime inicio,
            java.time.LocalDateTime fin,
            org.springframework.data.domain.Pageable pageable);

    /**
     * Valida la conexiÃ³n y credenciales configuradas para Twilio.
     * Realiza una llamada de prueba no destructiva (fetch) al API de Twilio.
     *
     * @return true si la conexiÃ³n y autenticaciÃ³n son correctas.
     */
    boolean validarConexionTwilio();
}
