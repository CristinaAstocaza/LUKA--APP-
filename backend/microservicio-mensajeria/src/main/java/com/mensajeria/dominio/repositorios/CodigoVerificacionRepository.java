package com.mensajeria.dominio.repositorios;

import com.mensajeria.dominio.entidades.CodigoVerificacion;
import com.libreria.comun.enums.PropositoCodigo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

/**
 * Repositorio para la gestiÃ³n de cÃ³digos de verificaciÃ³n OTP.
 * <p>
 * Extiende JpaSpecificationExecutor para soportar el Specification Pattern,
 * permitiendo auditorÃ­as y limpiezas dinÃ¡micas.
 * </p>
 * 
 * @version 1.2.0
 */
@Repository
public interface CodigoVerificacionRepository extends JpaRepository<CodigoVerificacion, UUID>, JpaSpecificationExecutor<CodigoVerificacion> {

    /**
     * Busca el cÃ³digo mÃ¡s reciente, no usado, filtrando por Usuario y PROPÃ“SITO.
     */
    Optional<CodigoVerificacion> findTopByUsuarioIdAndPropositoAndUsadoFalseOrderByFechaCreacionDesc(
            UUID usuarioId,
            PropositoCodigo proposito
    );

    Optional<CodigoVerificacion> findByIdAndCodigoAndUsadoFalse(UUID id, String codigo);
    
    /**
     * Limpieza profunda: Elimina cÃ³digos expirados Y cÃ³digos ya utilizados.
     */
    @Modifying
    @Query("DELETE FROM CodigoVerificacion c WHERE c.fechaExpiracion < :fecha OR c.usado = true")
    int eliminarCodigosObsoletos(@Param("fecha") LocalDateTime fecha);

    
    /**
     * Cuenta cuÃ¡ntos cÃ³digos ha solicitado un usuario para un propÃ³sito desde una fecha dada.
     */
    long countByUsuarioIdAndPropositoAndFechaCreacionAfter(UUID usuarioId, PropositoCodigo proposito, LocalDateTime fecha);
}
