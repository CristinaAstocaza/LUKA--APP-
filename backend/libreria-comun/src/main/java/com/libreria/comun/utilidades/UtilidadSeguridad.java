package com.libreria.comun.utilidades;

import com.libreria.comun.seguridad.DetallesUsuario;
import org.springframework.security.core.context.SecurityContextHolder;
import java.util.UUID;

/**
 * Utilidad para acceder de forma simplificada a la informaciÃ³n del usuario autenticado.
 * 
 */
public final class UtilidadSeguridad {

    private UtilidadSeguridad() {}

    /**
     * Obtiene el UUID del usuario actualmente autenticado en el hilo de la peticiÃ³n.
     * 
     * @return UUID del usuario.
     * @throws ClassCastException si el principal no es del tipo DetallesUsuario.
     */
    public static UUID obtenerUsuarioId() {
        DetallesUsuario detalles = (DetallesUsuario) SecurityContextHolder
                .getContext()
                .getAuthentication()
                .getPrincipal();
        return detalles.getUsuarioId();
    }

    /**
     * Obtiene el correo electrÃ³nico del usuario autenticado.
     * 
     * @return String email.
     */
    public static String obtenerUsuarioEmail() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }
}
