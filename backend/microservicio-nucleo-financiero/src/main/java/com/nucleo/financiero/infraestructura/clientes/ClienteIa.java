package com.nucleo.financiero.infraestructura.clientes;

import com.libreria.comun.dtos.RespuestaIaDTO;
import com.libreria.comun.dtos.SolicitudIaDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * Cliente Feign para la comunicaciÃ³n con el motor de Inteligencia Artificial (Python/FastAPI).
 * <p>
 * Este cliente facilita la integraciÃ³n sÃ­ncrona con el servicio de IA para obtener
 * recomendaciones financieras en tiempo real.
 * </p>
 * 
 */
@FeignClient(name = "microservicio-ia", url = "${URL_PROD_IA:http://localhost:8086}")
public interface ClienteIa {

    /**
     * EnvÃ­a una solicitud de anÃ¡lisis financiero al motor de IA.
     * 
     * @param solicitud DTO enriquecido con el historial y contexto del usuario.
     * @return DTO con el consejo generado por Gemini.
     */
    @PostMapping("/api/v1/ia/analizar")
    RespuestaIaDTO analizarFinanzas(@RequestBody SolicitudIaDTO solicitud);
}
