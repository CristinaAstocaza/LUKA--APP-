package com.auditoria.dominio.entidades;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Entidad de persistencia que representa un registro de auditorÃ­a general en el
 * sistema.
 * <p>
 * Esta clase mapea la tabla {@code registros_auditoria} y se utiliza para
 * capturar eventos significativos realizados por los usuarios en los distintos
 * mÃ³dulos de <b>Luka App</b>.
 * Incluye optimizaciones a nivel de base de datos mediante Ã­ndices para mejorar
 * la velocidad de bÃºsqueda por mÃ³dulo, fecha y usuario.
 * </p>
 * 
 * @version 1.1.0
 * @since 2026-05-10
 */
@Entity
@Table(name = "registros_auditoria", indexes = {
        @Index(name = "idx_auditoria_modulo", columnList = "modulo"),
        @Index(name = "idx_auditoria_fecha", columnList = "fecha_hora"),
        @Index(name = "idx_auditoria_modulo_fecha", columnList = "modulo, fecha_hora")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegistroAuditoria {

    /**
     * Identificador Ãºnico universal (UUID) del registro de auditorÃ­a.
     */
    @Id
    @GeneratedValue
    @Column(columnDefinition = "uuid", updatable = false, nullable = false)
    private UUID id;

    /**
     * Identificador Ãºnico (UUID) del usuario que realizÃ³ la acciÃ³n.
     */
    @Column(name = "usuario_id", nullable = false)
    private UUID usuarioId;

    /**
     * DescripciÃ³n breve de la acciÃ³n realizada (ej. "INICIO_SESION",
     * "CREACION_USUARIO").
     */
    @Column(name = "accion", nullable = false, length = 100)
    private String accion;

    /**
     * Nombre del mÃ³dulo o microservicio donde se originÃ³ el evento.
     */
    @Column(name = "modulo", nullable = false, length = 100)
    private String modulo;

    /**
     * DirecciÃ³n IP desde la cual se realizÃ³ la peticiÃ³n. Soporta formatos IPv4 e
     * IPv6.
     */
    @Column(name = "ip_origen", length = 45)
    private String ipOrigen;

    /**
     * Identificador de correlaciÃ³n Ãºnico para el seguimiento de la peticiÃ³n de extremo a extremo.
     */
    @Column(name = "correlation_id", length = 100)
    private String correlationId;

    /**
     * InformaciÃ³n adicional detallada sobre el evento en formato de texto libre o
     * JSON.
     */
    @Column(name = "detalles", columnDefinition = "TEXT")
    private String detalles;

    /**
     * Fecha en la que se registrÃ³ el evento de auditorÃ­a.
     */
    @Column(name = "fecha_hora", nullable = false)
    private LocalDate fechaHora;

    /**
     * MÃ©todo de ciclo de vida de JPA ejecutado antes de persistir la entidad.
     * <p>
     * Garantiza que el campo {@code fechaHora} siempre tenga un valor si no se
     * proporcionÃ³ uno manualmente.
     * </p>
     */
    @PrePersist
    protected void alCrear() {
        if (this.fechaHora == null) {
            this.fechaHora = LocalDate.now();
        }
    }
}
