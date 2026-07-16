package com.cliente.aplicacion.servicios;

import com.cliente.aplicacion.dtos.respuestas.RespuestaMetaAhorro;
import com.cliente.aplicacion.dtos.solicitudes.SolicitudMetaAhorro;
import com.cliente.aplicacion.excepciones.MetaNoEncontradaException;
import com.cliente.dominio.entidades.MetaAhorro;
import com.cliente.dominio.repositorios.MetaAhorroRepositorio;
import com.cliente.infraestructura.mensajeria.PublicadorAuditoria;
import com.libreria.comun.excepciones.ExcepcionAccesoDenegado;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Pruebas unitarias para {@link ServicioMetaAhorroImpl}.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ServicioMetaAhorroImpl — Pruebas Unitarias")
class ServicioMetaAhorroImplTest {

    @Mock
    private MetaAhorroRepositorio repositorio;

    @Mock
    private PublicadorAuditoria publicadorAuditoria;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private ServicioMetaAhorroImpl servicio;

    // ── Helper ────────────────────────────────────────────────────────────────
    private MetaAhorro crearMetaMock(UUID usuarioId) {
        MetaAhorro meta = new MetaAhorro();
        meta.setId(UUID.randomUUID());
        meta.setUsuarioId(usuarioId);
        meta.setNombre("Fondo Emergencia");
        meta.setMontoObjetivo(new BigDecimal("5000.00"));
        meta.setMontoActual(BigDecimal.ZERO);
        meta.setFechaObjetivo(LocalDate.now().plusMonths(6));
        meta.setCompletada(false);
        meta.setActiva(true);
        return meta;
    }

    // =========================================================================
    // crear()
    // =========================================================================

    @Test
    @DisplayName("crear: con datos válidos, debe persistir la meta y retornar DTO")
    void crear_conDatosValidos_debePersistirYRetornar() {
        UUID usuarioId = UUID.randomUUID();
        SolicitudMetaAhorro solicitud = new SolicitudMetaAhorro(
                "Fondo Emergencia", new BigDecimal("5000.00"), null,
                LocalDate.now().plusMonths(6), null, null, "Para imprevistos"
        );
        MetaAhorro guardada = crearMetaMock(usuarioId);

        when(repositorio.save(any(MetaAhorro.class))).thenReturn(guardada);

        RespuestaMetaAhorro resultado = servicio.crear(usuarioId, solicitud, "127.0.0.1");

        assertThat(resultado).isNotNull();
        assertThat(resultado.nombre()).isEqualTo("Fondo Emergencia");
        verify(repositorio).save(any(MetaAhorro.class));
        verify(publicadorAuditoria).publicarEventoExitoso(any());
    }

    // =========================================================================
    // actualizarMeta()
    // =========================================================================

    @Test
    @DisplayName("actualizarMeta: cuando meta no existe, debe lanzar MetaNoEncontradaException")
    void actualizarMeta_cuandoNoExiste_debeLanzarExcepcion() {
        UUID metaId = UUID.randomUUID();
        UUID usuarioId = UUID.randomUUID();
        SolicitudMetaAhorro solicitud = new SolicitudMetaAhorro(
                "Nueva", new BigDecimal("1000.00"), null,
                LocalDate.now().plusMonths(3), null, null, null
        );

        when(repositorio.findById(metaId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.actualizarMeta(metaId, usuarioId, solicitud, "ip"))
                .isInstanceOf(MetaNoEncontradaException.class);
    }

    @Test
    @DisplayName("actualizarMeta: cuando el usuario no es propietario, debe lanzar ExcepcionAccesoDenegado")
    void actualizarMeta_cuandoNoPropietario_debeLanzarAccesoDenegado() {
        UUID metaId     = UUID.randomUUID();
        UUID propietario = UUID.randomUUID();
        UUID intruso     = UUID.randomUUID();
        MetaAhorro meta  = crearMetaMock(propietario);

        when(repositorio.findById(metaId)).thenReturn(Optional.of(meta));

        SolicitudMetaAhorro solicitud = new SolicitudMetaAhorro(
                "Meta", new BigDecimal("500.00"), null,
                LocalDate.now().plusMonths(1), null, null, null
        );

        assertThatThrownBy(() -> servicio.actualizarMeta(metaId, intruso, solicitud, "ip"))
                .isInstanceOf(ExcepcionAccesoDenegado.class);
    }

    // =========================================================================
    // eliminarMeta()
    // =========================================================================

    @Test
    @DisplayName("eliminar: cuando la meta existe y el usuario es propietario, debe eliminar (lógicamente)")
    void eliminar_cuandoExisteYPropietario_debeEliminar() {
        UUID metaId    = UUID.randomUUID();
        UUID usuarioId = UUID.randomUUID();
        MetaAhorro meta = crearMetaMock(usuarioId);

        when(repositorio.findById(metaId)).thenReturn(Optional.of(meta));
        when(repositorio.save(any())).thenReturn(meta);

        servicio.eliminar(metaId, usuarioId, "127.0.0.1");

        assertThat(meta.getActiva()).isFalse();
        verify(repositorio).save(meta);
    }

    // =========================================================================
    // consultar()
    // =========================================================================

    @Test
    @DisplayName("consultar: cuando existe, debe retornar la meta correcta")
    void consultar_cuandoExiste_debeRetornar() {
        UUID metaId    = UUID.randomUUID();
        UUID usuarioId = UUID.randomUUID();
        MetaAhorro meta = crearMetaMock(usuarioId);

        when(repositorio.findById(metaId)).thenReturn(Optional.of(meta));

        RespuestaMetaAhorro resultado = servicio.consultar(metaId, usuarioId);

        assertThat(resultado).isNotNull();
        assertThat(resultado.nombre()).isEqualTo("Fondo Emergencia");
    }

    @Test
    @DisplayName("consultar: cuando no existe, debe lanzar MetaNoEncontradaException")
    void consultar_cuandoNoExiste_debeLanzarExcepcion() {
        UUID metaId    = UUID.randomUUID();
        UUID usuarioId = UUID.randomUUID();

        when(repositorio.findById(metaId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.consultar(metaId, usuarioId))
                .isInstanceOf(MetaNoEncontradaException.class);
    }

    // =========================================================================
    // listar()
    // =========================================================================

    @Test
    @DisplayName("listar: debe retornar página de metas del usuario")
    void listar_debeRetornarPagina() {
        UUID usuarioId = UUID.randomUUID();
        MetaAhorro meta = crearMetaMock(usuarioId);
        Page<MetaAhorro> pagina = new PageImpl<>(List.of(meta));

        when(repositorio.findByUsuarioIdAndActivaTrueOrderByFechaCreacionDesc(eq(usuarioId), any(PageRequest.class)))
                .thenReturn(pagina);

        com.libreria.comun.respuesta.Paginacion<RespuestaMetaAhorro> resultado = servicio.listar(
                usuarioId, PageRequest.of(0, 10)
        );

        assertThat(resultado.contenido()).hasSize(1);
    }
}
