package com.cliente.aplicacion.servicios;

import com.cliente.aplicacion.dtos.respuestas.RespuestaLimiteGasto;
import com.cliente.aplicacion.dtos.solicitudes.SolicitudLimiteGasto;
import com.cliente.aplicacion.excepciones.LimiteGastoException;
import com.cliente.aplicacion.excepciones.LimiteGastoNoEncontradoException;
import com.cliente.dominio.entidades.LimiteGasto;
import com.cliente.dominio.repositorios.LimiteGastoRepositorio;
import com.cliente.infraestructura.mensajeria.PublicadorAuditoria;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Pruebas unitarias para {@link ServicioLimiteGastoImpl}.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ServicioLimiteGastoImpl — Pruebas Unitarias")
class ServicioLimiteGastoImplTest {

    @Mock
    private LimiteGastoRepositorio repositorio;

    @Mock
    private PublicadorAuditoria publicadorAuditoria;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private ServicioLimiteGastoImpl servicio;

    // ── Helper para crear un LimiteGasto de prueba ──────────────────────────
    private LimiteGasto crearLimiteMock(UUID usuarioId, boolean activo) {
        LimiteGasto limite = new LimiteGasto();
        limite.setId(UUID.randomUUID());
        limite.setUsuarioId(usuarioId);
        limite.setNombre("Presupuesto Mensual");
        limite.setMontoLimite(new BigDecimal("1000.00"));
        limite.setPorcentajeAlerta(80);
        limite.setFechaInicio(LocalDate.now().minusDays(1));
        limite.setFechaFin(LocalDate.now().plusMonths(1));
        limite.setActivo(activo);
        return limite;
    }

    // =========================================================================
    // crear()
    // =========================================================================

    @Test
    @DisplayName("crear: cuando no hay límite activo, debe crear uno nuevo y retornar DTO")
    void crear_cuandoNoHayLimiteActivo_debeCrearYRetornar() {
        UUID usuarioId = UUID.randomUUID();
        SolicitudLimiteGasto solicitud = new SolicitudLimiteGasto(
                "Presupuesto Mensual", new BigDecimal("1500.00"), 80, null, null
        );
        LimiteGasto guardado = crearLimiteMock(usuarioId, true);

        when(repositorio.findByUsuarioIdAndActivoTrue(usuarioId)).thenReturn(Optional.empty());
        when(repositorio.save(any(LimiteGasto.class))).thenReturn(guardado);

        RespuestaLimiteGasto resultado = servicio.crear(usuarioId, solicitud, "127.0.0.1");

        assertThat(resultado).isNotNull();
        assertThat(resultado.activo()).isTrue();
        verify(repositorio).desactivarLimitesAnteriores(usuarioId);
        verify(repositorio).save(any(LimiteGasto.class));
    }

    @Test
    @DisplayName("crear: cuando solicitud es null, debe lanzar IllegalArgumentException")
    void crear_cuandoSolicitudNull_debeLanzarExcepcion() {
        UUID usuarioId = UUID.randomUUID();

        assertThatThrownBy(() -> servicio.crear(usuarioId, null, "127.0.0.1"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("crear: cuando ya existe límite activo y vigente, debe lanzar LimiteGastoException")
    void crear_cuandoLimiteActivoVigente_debeLanzarExcepcion() {
        UUID usuarioId = UUID.randomUUID();
        LimiteGasto limiteVigente = crearLimiteMock(usuarioId, true);
        SolicitudLimiteGasto solicitud = new SolicitudLimiteGasto(
                "Nuevo", new BigDecimal("500.00"), 70, null, null
        );

        when(repositorio.findByUsuarioIdAndActivoTrue(usuarioId))
                .thenReturn(Optional.of(limiteVigente));

        assertThatThrownBy(() -> servicio.crear(usuarioId, solicitud, "127.0.0.1"))
                .isInstanceOf(LimiteGastoException.class)
                .hasMessageContaining("activo y vigente");
    }

    // =========================================================================
    // obtenerActivo()
    // =========================================================================

    @Test
    @DisplayName("obtenerActivo: cuando existe, debe retornar el límite activo")
    void obtenerActivo_cuandoExiste_debeRetornar() {
        UUID usuarioId = UUID.randomUUID();
        LimiteGasto limite = crearLimiteMock(usuarioId, true);

        when(repositorio.findByUsuarioIdAndActivoTrue(usuarioId))
                .thenReturn(Optional.of(limite));

        RespuestaLimiteGasto resultado = servicio.obtenerActivo(usuarioId);

        assertThat(resultado).isNotNull();
        assertThat(resultado.activo()).isTrue();
    }

    @Test
    @DisplayName("obtenerActivo: cuando no existe, debe lanzar LimiteGastoNoEncontradoException")
    void obtenerActivo_cuandoNoExiste_debeLanzarExcepcion() {
        UUID usuarioId = UUID.randomUUID();
        when(repositorio.findByUsuarioIdAndActivoTrue(usuarioId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.obtenerActivo(usuarioId))
                .isInstanceOf(LimiteGastoNoEncontradoException.class);
    }

    // =========================================================================
    // listarHistorial()
    // =========================================================================

    @Test
    @DisplayName("listarHistorial: debe retornar todos los límites ordenados por fecha de creación")
    void listarHistorial_debeRetornarLista() {
        UUID usuarioId = UUID.randomUUID();
        LimiteGasto l1 = crearLimiteMock(usuarioId, false);
        LimiteGasto l2 = crearLimiteMock(usuarioId, true);

        when(repositorio.findByUsuarioIdOrderByFechaCreacionDesc(usuarioId))
                .thenReturn(List.of(l1, l2));

        List<RespuestaLimiteGasto> resultado = servicio.listarHistorial(usuarioId);

        assertThat(resultado).hasSize(2);
    }

    // =========================================================================
    // eliminar()
    // =========================================================================

    @Test
    @DisplayName("eliminar: cuando existe límite activo, debe desactivarlo (eliminación lógica)")
    void eliminar_cuandoExiste_debeDesactivar() {
        UUID usuarioId = UUID.randomUUID();
        LimiteGasto limite = crearLimiteMock(usuarioId, true);

        when(repositorio.findByUsuarioIdAndActivoTrue(usuarioId))
                .thenReturn(Optional.of(limite));
        when(repositorio.save(any())).thenReturn(limite);

        servicio.eliminar(usuarioId, "127.0.0.1");

        assertThat(limite.isActivo()).isFalse();
        verify(repositorio).save(limite);
    }

    @Test
    @DisplayName("eliminar: cuando no existe límite activo, debe lanzar LimiteGastoNoEncontradoException")
    void eliminar_cuandoNoExiste_debeLanzarExcepcion() {
        UUID usuarioId = UUID.randomUUID();
        when(repositorio.findByUsuarioIdAndActivoTrue(usuarioId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.eliminar(usuarioId, "127.0.0.1"))
                .isInstanceOf(LimiteGastoNoEncontradoException.class);
    }

    // =========================================================================
    // obtenerActivoInterno()
    // =========================================================================

    @Test
    @DisplayName("obtenerActivoInterno: cuando no existe, debe retornar null sin excepción")
    void obtenerActivoInterno_cuandoNoExiste_debeRetornarNull() {
        UUID usuarioId = UUID.randomUUID();
        when(repositorio.findByUsuarioIdAndActivoTrue(usuarioId)).thenReturn(Optional.empty());

        RespuestaLimiteGasto resultado = servicio.obtenerActivoInterno(usuarioId);

        assertThat(resultado).isNull();
    }
}
