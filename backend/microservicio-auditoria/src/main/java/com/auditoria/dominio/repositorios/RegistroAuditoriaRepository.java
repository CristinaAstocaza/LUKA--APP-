package com.auditoria.dominio.repositorios;

import com.auditoria.dominio.entidades.RegistroAuditoria;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

/**
 * Repositorio para la gestiÃ³n de persistencia de la entidad
 * {@link RegistroAuditoria}.
 * <p>
 * Proporciona los mÃ©todos estÃ¡ndar de CRUD mediante Spring Data JPA y define
 * consultas
 * especializadas para la explotaciÃ³n de datos de auditorÃ­a, permitiendo
 * filtrado
 * dinÃ¡mico y paginaciÃ³n sobre los eventos registrados.
 * </p>
 * 
 * @version 1.1.0
 * @since 2026-05-10
 */
public interface RegistroAuditoriaRepository extends JpaRepository<RegistroAuditoria, UUID>,
                org.springframework.data.jpa.repository.JpaSpecificationExecutor<RegistroAuditoria> {
}
