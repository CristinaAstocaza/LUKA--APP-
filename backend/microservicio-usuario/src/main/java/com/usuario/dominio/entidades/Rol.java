package com.usuario.dominio.entidades;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

/**
 * Entidad de Rol. Define los niveles de acceso disponibles en el sistema.
 * <p>
 * Los roles soportados son: FREE, PRO, PREMIUM, ADMIN y ADMINISTRADOR.
 * </p>
 * 
 * @version 1.1.0
 */
@Entity
@Table(name = "roles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Rol {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "uuid", updatable = false, nullable = false)
    private UUID id;

    /**
     * Nombre del rol. ConvenciÃ³n Spring Security: prefijo ROLE_.
     */
    @Column(nullable = false, unique = true, length = 50)
    private String nombre;

    // -------------------------------------------------------------------------
    // Enum de referencia para uso seguro en cÃ³digo
    // -------------------------------------------------------------------------
    public enum NombreRol {
        ROLE_ADMIN,
        ROLE_ADMINISTRADOR,
        ROLE_FREE,
        ROLE_PRO,
        ROLE_PREMIUM
    }
}
