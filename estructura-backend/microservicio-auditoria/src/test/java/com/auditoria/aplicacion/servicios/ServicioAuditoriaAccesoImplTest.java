package com.auditoria.aplicacion.servicios;

import com.auditoria.aplicacion.puertos.ServicioSeguridadAuditoria;
import com.auditoria.dominio.entidades.AuditoriaAcceso;
import com.auditoria.dominio.repositorios.AuditoriaAccesoRepository;
import com.libreria.comun.dtos.EventoAccesoDTO;
import com.libreria.comun.enums.EstadoEvento;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Pruebas unitarias para {@link ServicioAuditoriaAccesoImpl}.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ServicioAuditoriaAccesoImpl — Pruebas Unitarias")
class ServicioAuditoriaAccesoImplTest {

    @Mock
    private AuditoriaAccesoRepository repositorio;

    @Mock
    private ServicioSeguridadAuditoria servicioSeguridad;

    @InjectMocks
    private ServicioAuditoriaAccesoImpl servicio;

    // ── Helper ────────────────────────────────────────────────────────────────
    private EventoAccesoDTO crearEventoDTO(EstadoEvento estado, String correlationId) {
        return new EventoAccesoDTO(
                UUID.randomUUID(), "192.168.1.1",
                "Mozilla/5.0", estado, null,
                LocalDateTime.now(), correlationId);
    }

    private AuditoriaAcceso crearAccesoMock(EstadoEvento estado) {
        AuditoriaAcceso acceso = new AuditoriaAcceso();
        acceso.setId(UUID.randomUUID());
        acceso.setUsuarioId(UUID.randomUUID());
        acceso.setIpOrigen("192.168.1.1");
        acceso.setEstado(estado);
        acceso.setFecha(LocalDateTime.now());
        return acceso;
    }

    // =========================================================================
    // registrarAcceso()
    // =========================================================================

    @Test
    @DisplayName("registrarAcceso: sin correlationId, debe guardar el evento correctamente")
    void registrarAcceso_sinCorrelationId_debeGuardar() {
        EventoAccesoDTO dto = crearEventoDTO(EstadoEvento.EXITO, null);
        AuditoriaAcceso guardado = crearAccesoMock(EstadoEvento.EXITO);

        when(repositorio.save(any(AuditoriaAcceso.class))).thenReturn(guardado);

        EventoAccesoDTO resultado = servicio.registrarAcceso(dto);

        assertThat(resultado).isNotNull();
        verify(repositorio).save(any(AuditoriaAcceso.class));
        // Cuando es EXITO no debe verificar intentos fallidos
        verify(servicioSeguridad, never()).verificarIntentoFallido(any());
    }

    @Test
    @DisplayName("registrarAcceso: con correlationId duplicado, debe aplicar idempotencia y no guardar")
    void registrarAcceso_conCorrelationIdDuplicado_debeIgnorar() {
        String correlationId = UUID.randomUUID().toString();
        EventoAccesoDTO dto = crearEventoDTO(EstadoEvento.EXITO, correlationId);

        when(repositorio.existsByCorrelationId(correlationId)).thenReturn(true);

        EventoAccesoDTO resultado = servicio.registrarAcceso(dto);

        assertThat(resultado).isEqualTo(dto);
        verify(repositorio, never()).save(any());
    }

    @Test
    @DisplayName("registrarAcceso: cuando el estado es FALLO, debe verificar intento fallido en seguridad")
    void registrarAcceso_cuandoFallo_debeVerificarIntentoFallido() {
        EventoAccesoDTO dto = crearEventoDTO(EstadoEvento.FALLO, null);
        AuditoriaAcceso guardado = crearAccesoMock(EstadoEvento.FALLO);

        when(repositorio.save(any(AuditoriaAcceso.class))).thenReturn(guardado);

        servicio.registrarAcceso(dto);

        // Debe delegar en el servicio de seguridad para verificar si bloquear la IP
        verify(servicioSeguridad).verificarIntentoFallido("192.168.1.1");
    }

    // =========================================================================
    // listarTodo() y listarPorUsuario()
    // =========================================================================

    @Test
    @DisplayName("listarTodo: debe retornar página de eventos de acceso")
    void listarTodo_debeRetornarPagina() {
        AuditoriaAcceso acceso = crearAccesoMock(EstadoEvento.EXITO);
        Page<AuditoriaAcceso> pagina = new PageImpl<>(List.of(acceso));
        PageRequest paginacion = PageRequest.of(0, 10);

        when(repositorio.findAll(paginacion)).thenReturn(pagina);

        Page<EventoAccesoDTO> resultado = servicio.listarTodo(paginacion);

        assertThat(resultado).hasSize(1);
    }

    @Test
    @DisplayName("listarPorUsuario: debe retornar solo eventos del usuario dado")
    void listarPorUsuario_debeRetornarEventosDelUsuario() {
        UUID usuarioId = UUID.randomUUID();
        AuditoriaAcceso acceso = crearAccesoMock(EstadoEvento.EXITO);
        acceso.setUsuarioId(usuarioId);
        Page<AuditoriaAcceso> pagina = new PageImpl<>(List.of(acceso));
        PageRequest paginacion = PageRequest.of(0, 10);

        when(repositorio.findByUsuarioIdOrderByFechaDesc(usuarioId, paginacion))
                .thenReturn(pagina);

        Page<EventoAccesoDTO> resultado = servicio.listarPorUsuario(usuarioId, paginacion);

        assertThat(resultado).hasSize(1);
    }

    // =========================================================================
    // limpiarRegistrosAntiguos()
    // =========================================================================

    @Test
    @DisplayName("limpiarRegistrosAntiguos: debe eliminar registros anteriores al umbral de días")
    void limpiarRegistrosAntiguos_debeEliminarSegunDias() {
        when(repositorio.eliminarRegistrosAnterioresA(any(LocalDateTime.class))).thenReturn(5);

        int eliminados = servicio.limpiarRegistrosAntiguos(30);

        assertThat(eliminados).isEqualTo(5);
        verify(repositorio).eliminarRegistrosAnterioresA(any(LocalDateTime.class));
    }

    @Test
    @DisplayName("limpiarRegistrosAntiguos: cuando no hay registros antiguos, debe retornar 0")
    void limpiarRegistrosAntiguos_sinRegistrosAnuguos_debeRetornarCero() {
        when(repositorio.eliminarRegistrosAnterioresA(any(LocalDateTime.class))).thenReturn(0);

        int eliminados = servicio.limpiarRegistrosAntiguos(90);

        assertThat(eliminados).isZero();
    }
}
