package com.cliente.aplicacion.servicios;

import com.cliente.aplicacion.dtos.respuestas.RespuestaDatosPersonales;
import com.cliente.aplicacion.dtos.solicitudes.SolicitudDatosPersonales;
import com.cliente.aplicacion.eventos.EventoContextoActualizado;
import com.cliente.aplicacion.excepciones.DatosPersonalesNoEncontradosException;
import com.cliente.aplicacion.excepciones.DniDuplicadoException;
import com.cliente.dominio.entidades.DatosPersonales;
import com.cliente.dominio.repositorios.DatosPersonalesRepositorio;
import com.cliente.infraestructura.mensajeria.PublicadorAuditoria;
import com.libreria.comun.dtos.EventoAuditoriaDTO;
import com.libreria.comun.excepciones.ExcepcionAccesoDenegado;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Pruebas unitarias para {@link ServicioDatosPersonalesImpl}.
 * Se mockean todas las dependencias de infraestructura (repositorio, mensajería, eventos).
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ServicioDatosPersonalesImpl — Pruebas Unitarias")
class ServicioDatosPersonalesImplTest {

    // ── Mocks de dependencias ─────────────────────────────────────────────────
    @Mock
    private DatosPersonalesRepositorio repositorio;

    @Mock
    private PublicadorAuditoria publicadorAuditoria;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    // ── Clase bajo prueba ─────────────────────────────────────────────────────
    @InjectMocks
    private ServicioDatosPersonalesImpl servicio;

    // =========================================================================
    // crearPerfil()
    // =========================================================================

    @Test
    @DisplayName("crearPerfil: cuando el perfil NO existe, debe crear uno nuevo y retornarlo")
    void crearPerfil_cuandoNoExiste_debeCrearYRetornarDTO() {
        UUID usuarioId = UUID.randomUUID();
        DatosPersonales nuevo = DatosPersonales.builder()
                .usuarioId(usuarioId)
                .datosCompletos(false)
                .build();

        when(repositorio.findByUsuarioId(usuarioId)).thenReturn(Optional.empty());
        when(repositorio.save(any(DatosPersonales.class))).thenReturn(nuevo);

        RespuestaDatosPersonales resultado = servicio.crearPerfil(usuarioId);

        assertThat(resultado).isNotNull();
        assertThat(resultado.datosCompletos()).isFalse();
        verify(repositorio).save(any(DatosPersonales.class));
        verify(publicadorAuditoria).publicarEventoExitoso(any(EventoAuditoriaDTO.class));
    }

    @Test
    @DisplayName("crearPerfil: cuando el perfil YA existe, debe retornar el existente sin crear otro")
    void crearPerfil_cuandoYaExiste_debeRetornarExistenteSinCrear() {
        UUID usuarioId = UUID.randomUUID();
        DatosPersonales existente = DatosPersonales.builder()
                .usuarioId(usuarioId)
                .nombres("Juan")
                .apellidos("Pérez")
                .datosCompletos(false)
                .build();

        when(repositorio.findByUsuarioId(usuarioId)).thenReturn(Optional.of(existente));

        RespuestaDatosPersonales resultado = servicio.crearPerfil(usuarioId);

        assertThat(resultado).isNotNull();
        assertThat(resultado.nombres()).isEqualTo("Juan");
        // NO debe guardar un registro nuevo
        verify(repositorio, never()).save(any());
    }

    // =========================================================================
    // actualizar()
    // =========================================================================

    @Test
    @DisplayName("actualizar: cuando el usuario es propietario, debe aplicar cambios y retornar DTO")
    void actualizar_cuandoUsuarioPropietario_debeActualizarYRetornar() {
        UUID usuarioId = UUID.randomUUID();
        DatosPersonales existente = DatosPersonales.builder()
                .usuarioId(usuarioId)
                .build();
        SolicitudDatosPersonales solicitud = new SolicitudDatosPersonales(
                "12345678", "Ana", "García", "FEMENINO", 25,
                "999888777", null, "Perú", "Lima"
        );

        when(repositorio.findByUsuarioId(usuarioId)).thenReturn(Optional.of(existente));
        when(repositorio.existsByDni("12345678")).thenReturn(false);
        when(repositorio.save(any(DatosPersonales.class))).thenReturn(existente);

        RespuestaDatosPersonales resultado = servicio.actualizar(
                usuarioId, usuarioId, solicitud, "192.168.1.1"
        );

        assertThat(resultado).isNotNull();
        verify(repositorio).save(existente);
        verify(eventPublisher).publishEvent(any(EventoContextoActualizado.class));
    }

    @Test
    @DisplayName("actualizar: cuando el token no coincide con la ruta, debe lanzar ExcepcionAccesoDenegado")
    void actualizar_cuandoUsuarioNoPropietario_debeLanzarAccesoDenegado() {
        UUID usuarioIdRuta  = UUID.randomUUID();
        UUID usuarioIdToken = UUID.randomUUID(); // diferente
        SolicitudDatosPersonales solicitud = new SolicitudDatosPersonales(
                null, "Pedro", null, null, null, null, null, null, null
        );

        assertThatThrownBy(() ->
                servicio.actualizar(usuarioIdRuta, usuarioIdToken, solicitud, "10.0.0.1")
        ).isInstanceOf(ExcepcionAccesoDenegado.class);

        verify(repositorio, never()).save(any());
    }

    @Test
    @DisplayName("actualizar: cuando el DNI ya existe en otro usuario, debe lanzar DniDuplicadoException")
    void actualizar_cuandoDniDuplicado_debeLanzarExcepcion() {
        UUID usuarioId = UUID.randomUUID();
        DatosPersonales existente = DatosPersonales.builder()
                .usuarioId(usuarioId)
                .dni("00000000") // dni actual diferente
                .build();
        SolicitudDatosPersonales solicitud = new SolicitudDatosPersonales(
                "99999999", null, null, null, null, null, null, null, null
        );

        when(repositorio.findByUsuarioId(usuarioId)).thenReturn(Optional.of(existente));
        when(repositorio.existsByDni("99999999")).thenReturn(true); // ya existe

        assertThatThrownBy(() ->
                servicio.actualizar(usuarioId, usuarioId, solicitud, "10.0.0.1")
        ).isInstanceOf(DniDuplicadoException.class);
    }

    // =========================================================================
    // consultar()
    // =========================================================================

    @Test
    @DisplayName("consultar: cuando no existe perfil, debe lanzar DatosPersonalesNoEncontradosException")
    void consultar_cuandoNoExiste_debeLanzarExcepcion() {
        UUID usuarioId = UUID.randomUUID();
        when(repositorio.findByUsuarioId(usuarioId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.consultar(usuarioId, usuarioId))
                .isInstanceOf(DatosPersonalesNoEncontradosException.class);
    }

    @Test
    @DisplayName("consultar: cuando existe perfil, debe retornar el DTO correctamente")
    void consultar_cuandoExiste_debeRetornarDTO() {
        UUID usuarioId = UUID.randomUUID();
        DatosPersonales perfil = DatosPersonales.builder()
                .usuarioId(usuarioId)
                .nombres("Luis")
                .apellidos("Rodríguez")
                .datosCompletos(false)
                .build();

        when(repositorio.findByUsuarioId(usuarioId)).thenReturn(Optional.of(perfil));

        RespuestaDatosPersonales resultado = servicio.consultar(usuarioId, usuarioId);

        assertThat(resultado.nombres()).isEqualTo("Luis");
        assertThat(resultado.apellidos()).isEqualTo("Rodríguez");
    }

    // =========================================================================
    // consultarInterno()
    // =========================================================================

    @Test
    @DisplayName("consultarInterno: cuando no existe perfil, debe retornar null (sin excepción)")
    void consultarInterno_cuandoNoExiste_debeRetornarNull() {
        UUID usuarioId = UUID.randomUUID();
        when(repositorio.findByUsuarioId(usuarioId)).thenReturn(Optional.empty());

        RespuestaDatosPersonales resultado = servicio.consultarInterno(usuarioId);

        assertThat(resultado).isNull();
    }

    // =========================================================================
    // actualizarTelefono()
    // =========================================================================

    @Test
    @DisplayName("actualizarTelefono: cuando el perfil existe, debe actualizar y publicar evento de contexto")
    void actualizarTelefono_debeActualizarYPublicarEvento() {
        UUID usuarioId = UUID.randomUUID();
        DatosPersonales perfil = DatosPersonales.builder()
                .usuarioId(usuarioId)
                .build();

        when(repositorio.findByUsuarioId(usuarioId)).thenReturn(Optional.of(perfil));
        when(repositorio.save(any(DatosPersonales.class))).thenReturn(perfil);

        servicio.actualizarTelefono(usuarioId, "987654321");

        ArgumentCaptor<DatosPersonales> captor = ArgumentCaptor.forClass(DatosPersonales.class);
        verify(repositorio).save(captor.capture());
        assertThat(captor.getValue().getTelefono()).isEqualTo("987654321");
        verify(eventPublisher).publishEvent(any(EventoContextoActualizado.class));
    }

    @Test
    @DisplayName("actualizarTelefono: cuando el perfil NO existe, debe lanzar RuntimeException")
    void actualizarTelefono_cuandoNoExiste_debeLanzarExcepcion() {
        UUID usuarioId = UUID.randomUUID();
        when(repositorio.findByUsuarioId(usuarioId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.actualizarTelefono(usuarioId, "999"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Perfil no encontrado");
    }
}
