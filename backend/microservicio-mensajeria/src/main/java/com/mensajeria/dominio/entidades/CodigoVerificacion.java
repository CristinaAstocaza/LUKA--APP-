package com.mensajeria.dominio.entidades;

import com.libreria.comun.enums.PropositoCodigo;
import com.libreria.comun.enums.TipoVerificacion;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entidad principal del microservicio. Almacena los cÃ³digos OTP de 6 dÃ­gitos
 * asociados a un usuario especÃ­fico. Vigencia por defecto: 10 minutos
 * (configurable).
 *
 */
@Entity
@Table(
        name = "codigos_verificacion",
        indexes = {
            @Index(name = "idx_codigo_otp", columnList = "codigo"),
            @Index(name = "idx_codigo_usuario_id", columnList = "usuario_id"),
            @Index(name = "idx_codigo_fecha_expira", columnList = "fecha_expiracion"),
            @Index(name = "idx_codigo_email", columnList = "email")
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CodigoVerificacion {

    @Id
    @GeneratedValue
    @Column(columnDefinition = "uuid", updatable = false, nullable = false)
    private UUID id;

    /**
     * Identificador del usuario en el Microservicio-Usuario. Campo OBLIGATORIO:
     * todo cÃ³digo debe estar ligado a un usuario del sistema.
     */
    @Column(name = "usuario_id", nullable = false, updatable = false)
    private UUID usuarioId;

    @Column(nullable = true, length = 150)
    private String email;

    @Column(name = "telefono", length = 20)
    private String telefono;

    @Column(nullable = false, length = 6)
    private String codigo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private TipoVerificacion tipo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private PropositoCodigo proposito;

    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private LocalDateTime fechaCreacion;

    @Column(name = "fecha_expiracion", nullable = false)
    private LocalDateTime fechaExpiracion;

    @Column(nullable = false)
    @Builder.Default
    private Boolean usado = false;

    @Column(name = "fecha_uso")
    private LocalDateTime fechaUso;

    // Lifecycle
    @PrePersist
    protected void alCrear() {
        fechaCreacion = LocalDateTime.now();
        // La expiraciÃ³n puede ser sobreescrita antes del persist si viene del servicio;
        // si sigue siendo null, asignamos el valor por defecto de 10 minutos.
        if (fechaExpiracion == null) {
            fechaExpiracion = LocalDateTime.now().plusMinutes(10);
        }
    }

    // â”€â”€â”€ MÃ©todos de dominio â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
    /**
     * EvalÃºa si el cÃ³digo ya pasÃ³ su ventana de validez.
     *
     * @return
     */
    public boolean isExpirado() {
        return LocalDateTime.now().isAfter(fechaExpiracion);
    }
    
    /**
     * ValidaciÃ³n optimizada: verifica si el cÃ³digo coincide, no ha sido usado,
     * no ha expirado y el propÃ³sito es el correcto.
     * @param codigoIngresado
     * @param propositoRequerido
     * @return 
     */
    public boolean esValidoPara(String codigoIngresado, PropositoCodigo propositoRequerido) {
        return !usado
                && !isExpirado()
                && this.codigo.equals(codigoIngresado)
                && this.proposito == propositoRequerido;
    }
}
