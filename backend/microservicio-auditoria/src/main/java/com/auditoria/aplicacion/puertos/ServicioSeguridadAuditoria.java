package com.auditoria.aplicacion.puertos;

import com.auditoria.aplicacion.dtos.RespuestaVerificacionIpDTO;

/**
 * Interfaz de puerto encargada de la polÃ­tica de seguridad y defensa perimetral.
 * <p>
 * Define los mÃ©todos para evaluar ataques de fuerza bruta, gestionar el bloqueo
 * de IPs y verificar el estado de las mismas frente a la lista negra.
 * </p>
 * 
 */
public interface ServicioSeguridadAuditoria {

    /**
     * EvalÃºa si una IP debe ser bloqueada tras un intento fallido de acceso.
     * 
     * @param ipOrigen DirecciÃ³n IP a evaluar.
     */
    void verificarIntentoFallido(String ipOrigen);

    /**
     * Comprueba si una IP estÃ¡ habilitada para realizar peticiones.
     * 
     * @param ip DirecciÃ³n IP a verificar.
     * @return DTO con el estado de la IP (LIBRE/BLOQUEADA).
     */
    RespuestaVerificacionIpDTO verificarEstadoIp(String ip);

    /**
     * Tarea de mantenimiento para remover bloqueos cuya fecha de expiraciÃ³n ha pasado.
     */
    void limpiarBloqueosExpirados();

    /**
     * Recupera una lista paginada de todos los bloqueos de IP (histÃ³ricos y activos).
     * 
     * @param paginacion Datos de paginaciÃ³n.
     * @return PÃ¡gina de registros de lista negra.
     */
    org.springframework.data.domain.Page<com.auditoria.dominio.entidades.ListaNegraIp> listarBloqueos(org.springframework.data.domain.Pageable paginacion);

    /**
     * Registra un bloqueo manual para una direcciÃ³n IP especÃ­fica.
     * 
     * @param ip        DirecciÃ³n IP a bloquear.
     * @param motivo    RazÃ³n del bloqueo.
     * @param minutos   DuraciÃ³n del bloqueo en minutos (0 o negativo para bloqueo indefinido).
     */
    void bloquearIpManualmente(String ip, String motivo, int minutos);

    /**
     * Remueve manualmente cualquier bloqueo activo para una IP especÃ­fica.
     * 
     * @param ip DirecciÃ³n IP a desbloquear.
     */
    void desbloquearIpManualmente(String ip);
}
