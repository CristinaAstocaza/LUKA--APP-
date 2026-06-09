package com.mensajeria.infraestructura.clientes;

import java.util.UUID;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;
import com.libreria.comun.respuesta.ResultadoApi;

/**
 * Feign Client para la sincronizaciÃ³n de datos personales con el ms-usuario.
 * <p>
 * Invocado por el ms-mensajeria cuando la validaciÃ³n OTP por SMS en el flujo
 * de recuperaciÃ³n de contraseÃ±a es exitosa, garantizando que el telÃ©fono
 * verificado quede persistido en el microservicio correspondiente.
 * </p>
 * <p>
 * Si el ms-usuario no responde, Resilience4j activa
 * {@link ClienteActualizarTelefonoFallback}, que devuelve
 * {@code "SINCRONIZACION_PENDIENTE"} sin bloquear el flujo del usuario.
 * </p>
 *
 * <p>
 * <strong>âš  Contrato pendiente:</strong> El endpoint
 * {@code PUT /api/v1/datos-personales/telefono/{usuarioId}} debe ser creado
 * en el {@code microservicio-usuario} antes del primer despliegue conjunto.
 * Mientras no exista, el fallback capturarÃ¡ el 404 automÃ¡ticamente.
 * Responsable: equipo de ms-usuario. Referencia: ADR-MENSAJERIA-001.
 * </p>
 *
 * @version 1.1.0
 */
@FeignClient(name = "microservicio-usuario", contextId = "clienteActualizarTelefono", url = "${URL_PROD_USUARIO:http://localhost:8081}", fallback = ClienteActualizarTelefonoFallback.class)
public interface ClienteActualizarTelefono {

    /**
     * Actualiza el nÃºmero de telÃ©fono verificado del usuario en el ms-usuario.
     * <p>
     * Este endpoint es el contrato definido por el equipo de arquitectura para
     * la sincronizaciÃ³n de datos personales tras la validaciÃ³n OTP. Si el
     * endpoint devuelve 404 (aÃºn no implementado), el fallback lo intercepta.
     * </p>
     *
     * @param usuarioId UUID del usuario cuyo telÃ©fono debe actualizarse; se
     *                  pasa como variable de ruta para identificaciÃ³n directa.
     * @param telefono  NÃºmero de telÃ©fono en formato E.164 ({@code +51XXXXXXXXX})
     *                  verificado mediante OTP y que debe persistirse.
     * @return Cadena de confirmaciÃ³n del ms-usuario, o
     *         {@code "SINCRONIZACION_PENDIENTE"} si el servicio no responde.
     */
    @PutMapping("/api/v1/datos-personales/telefono/{usuarioId}")
    ResultadoApi<String> sincronizarTelefono(
            @PathVariable("usuarioId") UUID usuarioId,
            @RequestParam("telefono") String telefono);
}
