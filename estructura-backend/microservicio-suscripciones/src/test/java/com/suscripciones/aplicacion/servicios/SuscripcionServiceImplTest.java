package com.suscripciones.aplicacion.servicios;

import com.fasterxml.jackson.databind.ObjectMapper;

import com.libreria.comun.utilidades.CalculadorFechasStrategy;
import com.suscripciones.aplicacion.dtos.RespuestaSuscripcion;
import com.suscripciones.aplicacion.dtos.SolicitudCrearSuscripcion;
import com.suscripciones.dominio.entidades.BandejaSalida;

import com.suscripciones.dominio.entidades.Suscripcion;
import com.suscripciones.dominio.excepciones.ExcepcionSuscripcionNoEncontrada;
import com.suscripciones.dominio.repositorios.BandejaSalidaRepository;
import com.suscripciones.dominio.repositorios.ClaveIdempotenciaRepository;
import com.suscripciones.dominio.repositorios.HistorialPagoSuscripcionRepository;
import com.suscripciones.dominio.repositorios.SuscripcionRepository;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Pruebas unitarias para {@link SuscripcionServiceImpl}.
 * Verifica la lógica de negocio del ciclo de vida de suscripciones.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("SuscripcionServiceImpl — Pruebas Unitarias")
class SuscripcionServiceImplTest {

    @Mock
    private SuscripcionRepository suscripcionRepository;

    @Mock
    private HistorialPagoSuscripcionRepository historialPagoSuscripcionRepository;

    @Mock
    private BandejaSalidaRepository bandejaSalidaRepository;

    @Mock
    private ClaveIdempotenciaRepository claveIdempotenciaRepository;

    @Spy
    private List<CalculadorFechasStrategy> estrategias = new java.util.ArrayList<>();

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private SuscripcionServiceImpl servicio;

    @BeforeEach
    void setUp() {
        CalculadorFechasStrategy mockEstrategia = mock(CalculadorFechasStrategy.class);
        lenient().when(mockEstrategia.soporta(any())).thenReturn(true);
        lenient().when(mockEstrategia.calcularSiguienteFechaPago(any())).thenReturn(LocalDate.now().plusMonths(1));
        estrategias.add(mockEstrategia);
    }

    // ── Helpers ────────────────────────────────────────────────────────────────
    private Suscripcion crearSuscripcionMock(UUID usuarioId) {
        Suscripcion s = new Suscripcion();
        s.setId(UUID.randomUUID());
        s.setUsuarioId(usuarioId);
        s.setNombre("Netflix Premium");
        s.setMonto(new BigDecimal("45.90"));
        s.setEstado("ACTIVA");
        s.setMetodoPago("TARJETA");
        s.setFechaInicio(LocalDate.now());
        s.setFechaVencimiento(LocalDate.now().plusMonths(1));
        s.setTipoEstrategia("CALENDARIO");
        s.setEliminado(false);
        return s;
    }

    private SolicitudCrearSuscripcion crearSolicitud(UUID usuarioId) {
        return new SolicitudCrearSuscripcion(
                usuarioId, "Netflix Premium",
                new BigDecimal("45.90"), "TARJETA",
                UUID.randomUUID(), "CALENDARIO", LocalDate.now(),
                LocalDate.now().plusMonths(1));
    }

    // =========================================================================
    // crearSuscripcion()
    // =========================================================================

    @Test
    @DisplayName("crearSuscripcion: con datos válidos, debe persistir y retornar DTO")
    void crearSuscripcion_conDatosValidos_debePersistirYRetornar() {
        UUID usuarioId = UUID.randomUUID();
        SolicitudCrearSuscripcion solicitud = crearSolicitud(usuarioId);
        Suscripcion guardada = crearSuscripcionMock(usuarioId);

        when(suscripcionRepository.save(any(Suscripcion.class))).thenReturn(guardada);

        RespuestaSuscripcion resultado = servicio.crearSuscripcion(solicitud);

        assertThat(resultado).isNotNull();
        assertThat(resultado.nombre()).isEqualTo("Netflix Premium");
        verify(suscripcionRepository).save(any(Suscripcion.class));
    }

    @Test
    @DisplayName("crearSuscripcion: con fechaVencimiento explícita, no debe usar estrategia de cálculo")
    void crearSuscripcion_conFechaVencimientoExplicita_noDebeUsarEstrategia() {
        UUID usuarioId = UUID.randomUUID();
        LocalDate fechaVencExplicita = LocalDate.now().plusMonths(3);
        SolicitudCrearSuscripcion solicitud = new SolicitudCrearSuscripcion(
                usuarioId, "Spotify", new BigDecimal("15.00"),
                "EFECTIVO", UUID.randomUUID(), null, LocalDate.now(), fechaVencExplicita);
        Suscripcion guardada = crearSuscripcionMock(usuarioId);

        when(suscripcionRepository.save(any(Suscripcion.class))).thenReturn(guardada);

        servicio.crearSuscripcion(solicitud);

        // La estrategia NO debe ser invocada si la fecha vencimiento se proporcionó
        verify(estrategias, never()).iterator();
        verify(suscripcionRepository).save(any(Suscripcion.class));
    }

    // =========================================================================
    // buscarPorId()
    // =========================================================================

    @Test
    @DisplayName("buscarPorId: cuando existe, debe retornar la suscripción")
    void buscarPorId_cuandoExiste_debeRetornar() {
        UUID id = UUID.randomUUID();
        UUID usuarioId = UUID.randomUUID();
        Suscripcion suscripcion = crearSuscripcionMock(usuarioId);

        when(suscripcionRepository.findById(id)).thenReturn(Optional.of(suscripcion));

        RespuestaSuscripcion resultado = servicio.buscarPorId(id);

        assertThat(resultado).isNotNull();
        assertThat(resultado.nombre()).isEqualTo("Netflix Premium");
    }

    @Test
    @DisplayName("buscarPorId: cuando no existe, debe lanzar ExcepcionSuscripcionNoEncontrada")
    void buscarPorId_cuandoNoExiste_debeLanzarExcepcion() {
        UUID id = UUID.randomUUID();
        when(suscripcionRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.buscarPorId(id))
                .isInstanceOf(ExcepcionSuscripcionNoEncontrada.class);
    }

    // =========================================================================
    // listarPorUsuario()
    // =========================================================================

    @Test
    @DisplayName("listarPorUsuario: debe retornar página de suscripciones del usuario")
    @SuppressWarnings("unchecked")
    void listarPorUsuario_debeRetornarPagina() {
        UUID usuarioId = UUID.randomUUID();
        Suscripcion s1 = crearSuscripcionMock(usuarioId);
        Page<Suscripcion> pagina = new PageImpl<>(List.of(s1));

        when(suscripcionRepository.findAll(any(Specification.class), any(PageRequest.class)))
                .thenReturn(pagina);

        Page<RespuestaSuscripcion> resultado = servicio.listarPorUsuario(
                usuarioId, null, null, null, PageRequest.of(0, 10));

        assertThat(resultado).hasSize(1);
    }

    // =========================================================================
    // cancelarSuscripcion()
    // =========================================================================

    @Test
    @DisplayName("cancelarSuscripcion: cuando existe y activa, debe cambiar estado a CANCELADA")
    void cancelarSuscripcion_cuandoExiste_debeCancelar() throws Exception {
        UUID id = UUID.randomUUID();
        UUID usuarioId = UUID.randomUUID();
        Suscripcion suscripcion = crearSuscripcionMock(usuarioId);

        when(suscripcionRepository.findById(id)).thenReturn(Optional.of(suscripcion));
        when(suscripcionRepository.save(any(Suscripcion.class))).thenReturn(suscripcion);
        when(bandejaSalidaRepository.save(any(BandejaSalida.class))).thenReturn(new BandejaSalida());
        when(objectMapper.writeValueAsString(any())).thenReturn("{}");

        servicio.cancelarSuscripcion(id);

        assertThat(suscripcion.getEstado()).isEqualTo("CANCELADA");
        verify(suscripcionRepository).save(suscripcion);
    }

    @Test
    @DisplayName("cancelarSuscripcion: cuando no existe, debe lanzar ExcepcionSuscripcionNoEncontrada")
    void cancelarSuscripcion_cuandoNoExiste_debeLanzarExcepcion() {
        UUID id = UUID.randomUUID();

        when(suscripcionRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.cancelarSuscripcion(id))
                .isInstanceOf(ExcepcionSuscripcionNoEncontrada.class);
    }
}
