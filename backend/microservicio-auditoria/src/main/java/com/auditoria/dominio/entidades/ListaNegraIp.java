package com.auditoria.dominio.entidades;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entidad de persistencia que gestiona el bloqueo de direcciones IP maliciosas.
 * <p>
 * Funciona como el catÃ¡logo central de seguridad que es consultado de forma
 * prioritaria por el <b>microservicio-gateway</b> antes de procesar cualquier
 * peticiÃ³n entrante a <b>Luka App</b>.
 * </p>
 *
 * @version 1.2.0
 * @since 2026-05-10
 */
@Entity
@Table(name = "lista_negra_ip", indexes = {
        @Index(name = "idx_lista_negra_ip", columnList = "ip"),
        @Index(name = "idx_lista_negra_expiracion", columnList = "fecha_expiracion")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ListaNegraIp {

    /**
     * Identificador Ãºnico del registro de bloqueo.
     */
    @Id
    @GeneratedValue
    @Column(columnDefinition = "uuid", updatable = false, nullable = false)
    private UUID id;

    /**
     * DirecciÃ³n IP bloqueada. Soporta formatos IPv4 e IPv6.
     */
    @Column(name = "ip", nullable = false, length = 45)
    private String ip;

    /**
     * DescripciÃ³n detallada del motivo que originÃ³ el bloqueo (ej. "MÃºltiples
     * fallos de autenticaciÃ³n").
     */
    @Column(name = "motivo", nullable = false, length = 300)
    private String motivo;

    /**
     * Fecha y hora en la que se registrÃ³ el bloqueo inicial.
     */
    @Column(name = "fecha_bloqueo", nullable = false)
    private LocalDateTime fechaBloqueo;

    /**
     * Marca temporal que indica cuÃ¡ndo caduca el bloqueo automÃ¡ticamente.
     * <p>
     * Un valor {@code null} indica que el bloqueo es de carÃ¡cter permanente
     * hasta que sea revocado manualmente por un administrador.
     * </p>
     */
    @Column(name = "fecha_expiracion")
    private LocalDateTime fechaExpiracion;

    // â”€â”€â”€ MÃ©todos de dominio â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    /**
     * EvalÃºa la vigencia actual del bloqueo.
     * 
     * @return {@code true} si el bloqueo es permanente o si la fecha de expiraciÃ³n
     *         es posterior a la hora actual; {@code false} en caso contrario.
     */
    public boolean estaActivo() {
        if (fechaExpiracion == null) {
            return true; // Bloqueo permanente
        }
        return LocalDateTime.now().isBefore(fechaExpiracion);
    }

    /**
     * Incrementa la duraciÃ³n del bloqueo activo.
     * 
     * @param minutos Cantidad de minutos a sumar a partir del instante actual
     *                para definir la nueva fecha de expiraciÃ³n.
     */
    public void extenderBloqueo(long minutos) {
        this.fechaExpiracion = LocalDateTime.now().plusMinutes(minutos);
    }

    /**
     * MÃ©todo de ciclo de vida de JPA ejecutado previo a la persistencia inicial.
     * <p>
     * Asigna automÃ¡ticamente la fecha de bloqueo actual si no ha sido definida.
     * </p>
     */
    @PrePersist
    protected void alCrear() {
        if (fechaBloqueo == null) {
            fechaBloqueo = LocalDateTime.now();
        }
    }
}
