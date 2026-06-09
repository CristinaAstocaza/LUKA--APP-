package com.cliente.aplicacion.eventos;

import java.util.UUID;

/**
 * Evento de dominio emitido cuando el contexto financiero de un usuario
 * ha sido modificado (perfil, metas, lÃ­mites).
 * <p>
 * Este evento es capturado por el {@code EscuchaSincronizacionIA}
 * en la fase {@code AFTER_COMMIT} de la transacciÃ³n, garantizando que
 * la publicaciÃ³n a RabbitMQ y el refresco de Redis solo ocurran si la
 * base de datos confirmÃ³ los cambios exitosamente.
 * </p>
 *
 * @version 1.1.0
 * @since 2026-05-10
 */
public class EventoContextoActualizado {

    private final UUID usuarioId;
    private final String origen;

    /**
     * Construye un nuevo evento de contexto actualizado.
     *
     * @param usuarioId ID del usuario cuyo contexto fue modificado.
     * @param origen    Identificador del servicio que disparÃ³ el cambio
     *                  (ej: "PERFIL_FINANCIERO", "META_AHORRO", "LIMITE_GASTO").
     */
    public EventoContextoActualizado(UUID usuarioId, String origen) {
        this.usuarioId = usuarioId;
        this.origen = origen;
    }

    /**
     * Obtiene el ID del usuario afectado.
     *
     * @return UUID del usuario.
     */
    public UUID getUsuarioId() {
        return usuarioId;
    }

    /**
     * Obtiene el origen del cambio para trazabilidad.
     *
     * @return Nombre del mÃ³dulo que disparÃ³ la actualizaciÃ³n.
     */
    public String getOrigen() {
        return origen;
    }
}
