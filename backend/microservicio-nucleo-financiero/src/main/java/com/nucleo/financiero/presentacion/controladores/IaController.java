package com.nucleo.financiero.presentacion.controladores;

import com.libreria.comun.dtos.RespuestaIaDTO;
import com.libreria.comun.dtos.SolicitudIaDTO;
import com.libreria.comun.respuesta.ResultadoApi;
import com.libreria.comun.utilidades.UtilidadIp;
import com.nucleo.financiero.aplicacion.puertos.IServicioIa;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controlador REST para la interacciÃ³n con el motor de Inteligencia Artificial
 * (LUKA-IA).
 * <p>
 * Este controlador expone endpoints para solicitar consejos financieros
 * personalizados
 * basados en el comportamiento transaccional del usuario. Utiliza el contrato
 * {@link IServicioIa}
 * para delegar el procesamiento analÃ­tico.
 * </p>
 *
 * @version 1.2.2
 */
@RestController
@RequestMapping("/api/v1/financiero/ia")
@RequiredArgsConstructor
@Slf4j
public class IaController {

    private final IServicioIa servicioIa;

    /**
     * Consulta al motor de IA para obtener un consejo financiero estratÃ©gico.
     * <p>
     * El flujo de negocio incluye:
     * 1. ExtracciÃ³n de la IP del cliente para auditorÃ­a.
     * 2. RecuperaciÃ³n del contexto financiero del usuario.
     * 3. GeneraciÃ³n de prompt dinÃ¡mico y consulta a Gemini.
     * 4. Registro del evento en el microservicio de auditorÃ­a vÃ­a RabbitMQ.
     * </p>
     * 
     * @param solicitud      Datos de la consulta (usuarioId, contexto opcional).
     * @param servletRequest PeticiÃ³n HTTP para extracciÃ³n de metadatos (IP).
     * @return ResponseEntity con {@link ResultadoApi} conteniendo la respuesta de
     *         la IA.
     */
    @PostMapping("/consultar")
    public ResponseEntity<ResultadoApi<RespuestaIaDTO>> consultarIa(
            @Valid @RequestBody SolicitudIaDTO solicitud,
            HttpServletRequest servletRequest) {

        log.info("Iniciando consulta de IA para usuarioId={}", solicitud.getIdUsuario());

        // Extraemos la IP real del cliente para trazabilidad
        String ipCliente = UtilidadIp.obtenerIpReal(servletRequest);

        RespuestaIaDTO respuesta = servicioIa.obtenerConsejoIA(solicitud, ipCliente);

        return ResponseEntity.ok(ResultadoApi.exito(respuesta, "Consejo de IA generado exitosamente"));
    }
}
