package com.nucleo.financiero.aplicacion.servicios;

import com.nucleo.financiero.aplicacion.dtos.solicitudes.SolicitudTransaccion;
import com.nucleo.financiero.aplicacion.dtos.respuestas.RespuestaTransaccion;
import com.nucleo.financiero.aplicacion.mappers.TransaccionMapper;
import com.nucleo.financiero.dominio.entidades.Categoria;
import com.nucleo.financiero.dominio.entidades.Categoria.TipoMovimiento;
import com.nucleo.financiero.dominio.entidades.Transaccion;
import com.nucleo.financiero.dominio.entidades.Transaccion.MetodoPago;
import com.nucleo.financiero.dominio.repositorios.CategoriaRepository;
import com.nucleo.financiero.dominio.repositorios.TransaccionRepository;
import com.nucleo.financiero.infraestructura.mensajeria.PublicadorAuditoria;
import com.nucleo.financiero.infraestructura.mensajeria.PublicadorFinanciero;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Pruebas unitarias para {@link TransaccionServiceImpl}.
 * Verifica el registro individual, en lote, consultas y eliminación de
 * transacciones.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("TransaccionServiceImpl — Pruebas Unitarias")
class TransaccionServiceImplTest {

        @Mock
        private TransaccionRepository transaccionRepository;

        @Mock
        private CategoriaRepository categoriaRepository;

        @Mock
        private PublicadorAuditoria publicadorAuditoria;

        @Mock
        private PublicadorFinanciero publicadorFinanciero;

        @Mock
        private TransaccionMapper transaccionMapper;

        @InjectMocks
        private TransaccionServiceImpl servicio;

        // ── Helpers ────────────────────────────────────────────────────────────────
        private SolicitudTransaccion crearSolicitud(UUID usuarioId) {
                return new SolicitudTransaccion(
                                usuarioId, "Supermercado Wong",
                                new BigDecimal("150.00"), TipoMovimiento.GASTO,
                                UUID.randomUUID(), MetodoPago.TARJETA,
                                "alimentacion,hogar", "Compras del mes",
                                LocalDateTime.now());
        }

        private Transaccion crearTransaccionMock(UUID usuarioId) {
                return Transaccion.builder()
                                .id(UUID.randomUUID())
                                .usuarioId(usuarioId)
                                .nombreCliente("Supermercado Wong")
                                .monto(new BigDecimal("150.00"))
                                .tipo(TipoMovimiento.GASTO)
                                .metodoPago(MetodoPago.TARJETA)
                                .fechaTransaccion(LocalDateTime.now())
                                .build();
        }

        private RespuestaTransaccion crearRespuestaMock(UUID transaccionId, UUID usuarioId) {
                return new RespuestaTransaccion(
                                transaccionId, "Supermercado Wong",
                                new BigDecimal("150.00"), TipoMovimiento.GASTO.name(),
                                "Comida", "🍔", LocalDateTime.now(),
                                MetodoPago.TARJETA.name(), "alimentacion", "Compras", "COMPLETADO");
        }

        // =========================================================================
        // registrar()
        // =========================================================================

        @Test
        @DisplayName("registrar: con solicitud válida, debe guardar la transacción y publicar eventos")
        void registrar_conSolicitudValida_debeGuardarYPublicar() {
                UUID usuarioId = UUID.randomUUID();
                UUID transacId = UUID.randomUUID();
                SolicitudTransaccion solicitud = crearSolicitud(usuarioId);
                Transaccion guardada = crearTransaccionMock(usuarioId);
                RespuestaTransaccion respuestaMock = crearRespuestaMock(transacId, usuarioId);

                when(categoriaRepository.findById(solicitud.categoriaId()))
                                .thenReturn(Optional.of(new Categoria()));
                when(transaccionRepository.save(any(Transaccion.class))).thenReturn(guardada);
                when(transaccionMapper.aDto(guardada)).thenReturn(respuestaMock);

                RespuestaTransaccion resultado = servicio.registrar(solicitud, "127.0.0.1");

                assertThat(resultado).isNotNull();
                verify(transaccionRepository).save(any(Transaccion.class));
                verify(publicadorAuditoria).publicarRegistro(any(), any(), any(), any());
                verify(publicadorFinanciero).publicarTransaccionRegistrada(any(), any(), any(), any(), any());
        }

        @Test
        @DisplayName("registrar: con solicitud null, debe lanzar IllegalArgumentException")
        void registrar_cuandoSolicitudNull_debeLanzarExcepcion() {
                assertThatThrownBy(() -> servicio.registrar(null, "127.0.0.1"))
                                .isInstanceOf(IllegalArgumentException.class)
                                .hasMessageContaining("nula");
        }

        // =========================================================================
        // registrarLote()
        // =========================================================================

        @Test
        @DisplayName("registrarLote: con lista vacía, debe lanzar IllegalArgumentException")
        void registrarLote_conListaVacia_debeLanzarExcepcion() {
                assertThatThrownBy(() -> servicio.registrarLote(List.of(), "127.0.0.1"))
                                .isInstanceOf(IllegalArgumentException.class)
                                .hasMessageContaining("vacía");
        }

        @Test
        @DisplayName("registrarLote: con más de 500 transacciones, debe lanzar IllegalArgumentException")
        void registrarLote_conMasDe500_debeLanzarExcepcion() {
                UUID usuarioId = UUID.randomUUID();
                List<SolicitudTransaccion> lista = java.util.Collections
                                .nCopies(501, crearSolicitud(usuarioId));

                assertThatThrownBy(() -> servicio.registrarLote(lista, "127.0.0.1"))
                                .isInstanceOf(IllegalArgumentException.class)
                                .hasMessageContaining("500");
        }

        // =========================================================================
        // listarHistorial()
        // =========================================================================

        @Test
        @DisplayName("listarHistorial: con usuarioId null, debe lanzar IllegalArgumentException")
        void listarHistorial_cuandoUsuarioIdNull_debeLanzarExcepcion() {
                assertThatThrownBy(() -> servicio.listarHistorial(
                                null, null, null, null, null,
                                PageRequest.of(0, 10), "127.0.0.1")).isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("listarHistorial: con paginación null, debe lanzar IllegalArgumentException")
        void listarHistorial_cuandoPaginacionNull_debeLanzarExcepcion() {
                UUID usuarioId = UUID.randomUUID();

                assertThatThrownBy(() -> servicio.listarHistorial(
                                usuarioId, null, null, null, null, null, "127.0.0.1"))
                                .isInstanceOf(IllegalArgumentException.class);
        }
}
