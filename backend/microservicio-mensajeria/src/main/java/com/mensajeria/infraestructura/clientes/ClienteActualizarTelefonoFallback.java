package com.mensajeria.infraestructura.clientes;

import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import com.libreria.comun.respuesta.ResultadoApi;

/**
 * Fallback de Resilience4j para {@link ClienteActualizarTelefono}.
 * <p>
 * Si el ms-usuario no responde o falla, esta implementaciÃ³n es invocada
 * automÃ¡ticamente. El flujo de negocio <strong>no muere</strong>: el OTP ya
 * fue validado y el usuario puede continuar. El fallo de sincronizaciÃ³n del
 * telÃ©fono se registra en logs para reintento manual o eventual reconciliaciÃ³n.
 * </p>
 *
 * @version 1.1.0
 */
@Component
@Slf4j
public class ClienteActualizarTelefonoFallback implements ClienteActualizarTelefono {

    /**
     * Fallback del endpoint de sincronizaciÃ³n de telÃ©fono.
     * Registra la sincronizaciÃ³n como pendiente sin lanzar excepciÃ³n,
     * para que el usuario no experimente un error en su flujo de activaciÃ³n.
     *
     * @param usuarioId UUID del usuario cuyo telÃ©fono no pudo sincronizarse.
     * @param telefono  NÃºmero en formato E.164 que debÃ­a guardarse en ms-cliente.
     * @return Mensaje estÃ¡tico indicando que la sincronizaciÃ³n quedÃ³ pendiente.
     */
    @Override
    public ResultadoApi<String> sincronizarTelefono(UUID usuarioId, String telefono) {
        log.error(
                "[FEIGN-FALLBACK] ms-cliente no disponible. SincronizaciÃ³n de telÃ©fono PENDIENTE para usuario: {}",
                usuarioId);
        return ResultadoApi.exito("SINCRONIZACION_PENDIENTE", "SincronizaciÃ³n diferida");
    }
}
