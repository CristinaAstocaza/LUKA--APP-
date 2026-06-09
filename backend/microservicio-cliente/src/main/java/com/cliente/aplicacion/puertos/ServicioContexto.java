package com.cliente.aplicacion.puertos;

import com.libreria.comun.dtos.ContextoEstrategicoIADTO;
import com.libreria.comun.dtos.ContextoUsuarioDTO;
import java.util.UUID;

/**
 * Interfaz de puerto para compartir los datos personales y financieros del cliente.
 *
 * @since 2026-05
 */
public interface ServicioContexto {

    /**
     * Retorna un contexto financiero ligero, estrictamente necesario para que
     * la IA evalÃºe la situaciÃ³n financiera del cliente.
     */
    ContextoEstrategicoIADTO obtenerContextoFinanciero(UUID usuarioId);

    /**
     * Retorna el contexto completo consolidado del usuario (Perfil, Metas, LÃ­mites).
     */
    ContextoUsuarioDTO obtenerContextoCompleto(UUID usuarioId);

    /**
     * Refresca el contexto de la IA en Redis.
     */
    void refrescarContextoRedis(UUID usuarioId);
}
