package com.auditoria.aplicacion.servicios;

import com.auditoria.aplicacion.dtos.RespuestaAuditoriaDetalladoDTO;

import com.auditoria.dominio.entidades.RegistroAuditoria;
import com.auditoria.dominio.repositorios.RegistroAuditoriaRepository;
import com.libreria.comun.dtos.EventoAuditoriaDTO;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Pruebas unitarias para {@link ServicioRegistroAuditoriaImpl}.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ServicioRegistroAuditoriaImpl — Pruebas Unitarias")
class ServicioRegistroAuditoriaImplTest {

    @Mock
    private RegistroAuditoriaRepository repositorioAuditoria;

    @InjectMocks
    private ServicioRegistroAuditoriaImpl servicio;

    // ── Helper ────────────────────────────────────────────────────────────────
    private EventoAuditoriaDTO crearEventoMock(UUID usuarioId) {
        return EventoAuditoriaDTO.crear(
                usuarioId, "LOGIN_EXITOSO", "MS-USUARIO", "192.168.1.1",
                "Usuario inició sesión");
    }

    private RegistroAuditoria crearRegistroMock(UUID usuarioId) {
        RegistroAuditoria r = new RegistroAuditoria();
        r.setId(UUID.randomUUID());
        r.setUsuarioId(usuarioId);
        r.setAccion("LOGIN_EXITOSO");
        r.setModulo("MS-USUARIO");
        r.setIpOrigen("192.168.1.1");
        r.setFechaHora(java.time.LocalDate.now());
        return r;
    }

    // =========================================================================
    // registrarEvento()
    // =========================================================================

    @Test
    @DisplayName("registrarEvento: con datos completos, debe guardar en BD y retornar el DTO")
    void registrarEvento_conDatosCompletos_debeGuardarYRetornar() {
        UUID usuarioId = UUID.randomUUID();
        EventoAuditoriaDTO evento = crearEventoMock(usuarioId);
        RegistroAuditoria guardado = crearRegistroMock(usuarioId);

        when(repositorioAuditoria.save(any(RegistroAuditoria.class))).thenReturn(guardado);

        EventoAuditoriaDTO resultado = servicio.registrarEvento(evento);

        assertThat(resultado).isNotNull();
        assertThat(resultado.accion()).isEqualTo("LOGIN_EXITOSO");
        verify(repositorioAuditoria).save(any(RegistroAuditoria.class));
    }

    @Test
    @DisplayName("registrarEvento: debe mapear correctamente acción y módulo a la entidad")
    void registrarEvento_debeMappearCamposCorrectamente() {
        UUID usuarioId = UUID.randomUUID();
        EventoAuditoriaDTO evento = crearEventoMock(usuarioId);
        RegistroAuditoria guardado = crearRegistroMock(usuarioId);

        when(repositorioAuditoria.save(any(RegistroAuditoria.class))).thenReturn(guardado);

        servicio.registrarEvento(evento);

        ArgumentCaptor<RegistroAuditoria> captor = ArgumentCaptor.forClass(RegistroAuditoria.class);
        verify(repositorioAuditoria).save(captor.capture());
        RegistroAuditoria guardadoCapturado = captor.getValue();

        assertThat(guardadoCapturado.getAccion()).isEqualTo("LOGIN_EXITOSO");
        assertThat(guardadoCapturado.getModulo()).isEqualTo("MS-USUARIO");
        assertThat(guardadoCapturado.getIpOrigen()).isEqualTo("192.168.1.1");
    }

    @Test
    @DisplayName("registrarEvento: cuando acción es null, debe usar valor por defecto 'ACCESO_SISTEMA'")
    void registrarEvento_cuandoAccionNull_debeUsarDefault() {
        UUID usuarioId = UUID.randomUUID();
        EventoAuditoriaDTO eventoSinAccion = new EventoAuditoriaDTO(
                usuarioId, null, "MS-USUARIO", "192.168.1.1", "detalle", java.time.LocalDate.now());
        RegistroAuditoria guardado = crearRegistroMock(usuarioId);
        guardado.setAccion("ACCESO_SISTEMA");

        when(repositorioAuditoria.save(any(RegistroAuditoria.class))).thenReturn(guardado);

        servicio.registrarEvento(eventoSinAccion);

        ArgumentCaptor<RegistroAuditoria> captor = ArgumentCaptor.forClass(RegistroAuditoria.class);
        verify(repositorioAuditoria).save(captor.capture());
        assertThat(captor.getValue().getAccion()).isEqualTo("ACCESO_SISTEMA");
    }

    // =========================================================================
    // listarRegistrosDetallados()
    // =========================================================================

    @Test
    @DisplayName("listarRegistrosDetallados: debe retornar página de registros mapeados a DTO")
    @SuppressWarnings("unchecked")
    void listarRegistrosDetallados_debeRetornarPaginaMapeada() {
        UUID usuarioId = UUID.randomUUID();
        RegistroAuditoria registro = crearRegistroMock(usuarioId);
        Page<RegistroAuditoria> pagina = new PageImpl<>(List.of(registro));

        when(repositorioAuditoria.findAll(any(Specification.class), any(PageRequest.class)))
                .thenReturn(pagina);

        Page<RespuestaAuditoriaDetalladoDTO> resultado = servicio.listarRegistrosDetallados(
                "MS-USUARIO", PageRequest.of(0, 10));

        assertThat(resultado).hasSize(1);
        assertThat(resultado.getContent().get(0).accion()).isEqualTo("LOGIN_EXITOSO");
    }

    @Test
    @DisplayName("listarRegistrosDetallados: sin módulo (null), debe retornar todos los registros")
    @SuppressWarnings("unchecked")
    void listarRegistrosDetallados_sinModulo_debeRetornarTodos() {
        UUID usuarioId = UUID.randomUUID();
        Page<RegistroAuditoria> pagina = new PageImpl<>(
                List.of(crearRegistroMock(usuarioId), crearRegistroMock(usuarioId)));

        when(repositorioAuditoria.findAll(any(Specification.class), any(PageRequest.class)))
                .thenReturn(pagina);

        Page<RespuestaAuditoriaDetalladoDTO> resultado = servicio.listarRegistrosDetallados(
                null, PageRequest.of(0, 10));

        assertThat(resultado).hasSize(2);
    }
}
