package com.usuario.infraestructura.clientes;

import com.usuario.aplicacion.dtos.solicitudes.SolicitudGenerarOtp;
import com.usuario.aplicacion.dtos.solicitudes.SolicitudValidarRecuperacion;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Fallback para el cliente Feign de MensajerÃ­a.
 *
 */
@Component
@Slf4j
public class ClienteMensajeriaFallback implements ClienteMensajeria {

    @Override
    public UUID generarCodigo(SolicitudGenerarOtp solicitud) {
        log.error("Fallo crÃ­tico: No se pudo conectar con ms-mensajeria para generar OTP. El mensaje quedarÃ¡ en cola si se usa RabbitMQ.");
        return null;
    }

    @Override
    public com.libreria.comun.respuesta.ResultadoApi<UUID> validarCodigoYObtenerUsuario(SolicitudValidarRecuperacion solicitud) {
        log.error("Fallo crÃ­tico: No se puede validar cÃ³digo de recuperaciÃ³n porque ms-mensajeria estÃ¡ offline.");
        return null; // AquÃ­ el servicio de autenticaciÃ³n debe manejar el null
    }

    @Override
    public com.libreria.comun.respuesta.ResultadoApi<com.usuario.aplicacion.dtos.respuestas.RespuestaValidacion> validarActivacion(com.usuario.aplicacion.dtos.solicitudes.SolicitudValidarCodigo solicitud) {
        log.error("Fallo crÃ­tico: No se puede validar cÃ³digo de activaciÃ³n porque ms-mensajeria estÃ¡ offline.");
        return null;
    }

    @Override
    public void validarLimite(com.usuario.aplicacion.dtos.solicitudes.SolicitudVerificarLimite solicitud) {
        log.warn("MS-MensajerÃ­a offline: Saltando validaciÃ³n de lÃ­mite de OTP para no bloquear al usuario.");
        // Al ser void, simplemente no hace nada y permite que el flujo continÃºe
    }
}
