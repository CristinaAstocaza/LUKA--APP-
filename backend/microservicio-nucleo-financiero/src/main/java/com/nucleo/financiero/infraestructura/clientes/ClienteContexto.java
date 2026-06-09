package com.nucleo.financiero.infraestructura.clientes;

import com.libreria.comun.dtos.ContextoUsuarioDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.UUID;

/**
 * Cliente Feign para la comunicaciÃ³n con el microservicio de Clientes.
 * <p>
 * Permite recuperar el contexto enriquecido del usuario (perfil, metas, lÃ­mites)
 * necesario para alimentar el motor de IA.
 * </p>
 * 
 */
@FeignClient(name = "microservicio-cliente", url = "${URL_PROD_CLIENTE:http://localhost:8083}")
public interface ClienteContexto{

    /**
     * Recupera el contexto consolidado de un usuario para fines analÃ­ticos.
     * 
     * @param usuarioId Identificador Ãºnico del usuario.
     * @return DTO con la informaciÃ³n de contexto financiero y personal.
     */
    @GetMapping("/api/v1/clientes/interno/contexto/{usuarioId}")
    ContextoUsuarioDTO obtenerContexto(@PathVariable("usuarioId") UUID usuarioId);
}
