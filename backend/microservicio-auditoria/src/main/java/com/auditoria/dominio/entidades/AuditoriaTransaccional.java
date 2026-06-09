package com.auditoria.dominio.entidades;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Entidad de persistencia encargada de la trazabilidad detallada de cambios en
 * el negocio.
 * <p>
 * Proporciona un registro histÃ³rico de las modificaciones realizadas sobre
 * entidades crÃ­ticas.
 * Almacena instantÃ¡neas del estado anterior y posterior en formato JSON,
 * permitiendo la
 * reconstrucciÃ³n de estados previos y cumpliendo con estÃ¡ndares internacionales
 * de auditorÃ­a y cumplimiento (como PCIDSS y SOX) requeridos en el sector
 * financiero.
 * </p>
 * 
 * @version 1.1.0
 * @since 2026-05-10
 */
@Entity
@Table(name = "auditoria_transaccional", indexes = {
        @Index(name = "idx_transac_usuario_id", columnList = "usuario_id"),
        @Index(name = "idx_transac_entidad_id", columnList = "entidad_id"),
        @Index(name = "idx_transac_servicio_origen", columnList = "servicio_origen"),
        @Index(name = "idx_transac_fecha", columnList = "fecha")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditoriaTransaccional {

    /**
     * Identificador Ãºnico universal (UUID) del registro transaccional.
     */
    @Id
    @GeneratedValue
    @Column(columnDefinition = "uuid", updatable = false, nullable = false)
    private UUID id;

    /**
     * Identificador Ãºnico (UUID) del usuario responsable de la modificaciÃ³n.
     */
    @Column(name = "usuario_id", nullable = false)
    private UUID usuarioId;

    /**
     * Identificador Ãºnico (UUID) de la entidad de negocio que fue afectada en su
     * microservicio de origen.
     */
    @Column(name = "entidad_id", nullable = false)
    private UUID entidadId;

    /**
     * Nombre del microservicio que generÃ³ el evento (ej:
     * "MICROSERVICIO-FINANCIERO").
     */
    @Column(name = "servicio_origen", nullable = false, length = 100)
    private String servicioOrigen;

    /**
     * Nombre tÃ©cnico de la entidad de negocio modificada (ej: "Transaccion",
     * "Cliente").
     */
    @Column(name = "entidad_afectada", nullable = false, length = 100)
    private String entidadAfectada;

    /**
     * DescripciÃ³n textual de la acciÃ³n ejecutada sobre la entidad.
     */
    @Column(name = "descripcion", nullable = false, length = 255)
    private String descripcion;

    /**
     * RepresentaciÃ³n serializada (JSON) del estado de la entidad antes del cambio.
     * Es vital para procesos de reversiÃ³n o anÃ¡lisis de discrepancias.
     */
    @Column(name = "valor_anterior", columnDefinition = "TEXT")
    private String valorAnterior;

    /**
     * RepresentaciÃ³n serializada (JSON) del nuevo estado de la entidad tras la
     * operaciÃ³n.
     * Permite auditar exactamente quÃ© campos fueron alterados.
     */
    @Column(name = "valor_nuevo", columnDefinition = "TEXT")
    private String valorNuevo;

    /**
     * Fecha de registro de la transacciÃ³n de auditorÃ­a.
     */
    @Column(name = "fecha", nullable = false)
    private LocalDate fecha;

    /**
     * MÃ©todo de ciclo de vida de JPA ejecutado antes de la persistencia.
     * <p>
     * Garantiza que el campo {@code fecha} estÃ© poblado automÃ¡ticamente al momento
     * de la creaciÃ³n.
     * </p>
     */
    @PrePersist
    protected void alCrear() {
        if (fecha == null) {
            fecha = LocalDate.now();
        }
    }
}
