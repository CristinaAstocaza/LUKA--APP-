package com.pagos.aplicacion.servicios;

import com.libreria.comun.respuesta.Paginacion;
import com.pagos.aplicacion.dtos.ResumenPagosDTO;
import com.pagos.aplicacion.enums.EstadoPago;
import com.pagos.dominio.entidades.Pago;
import com.pagos.dominio.repositorios.RepositorioDetallePago;
import com.pagos.dominio.repositorios.RepositorioPago;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;

import static org.mockito.Mockito.*;

/**
 * Pruebas unitarias para {@link ServicioAdminPagosImpl}.
 * Verifica la consulta administrativa de pagos y el resumen general del
 * sistema.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ServicioAdminPagosImpl — Pruebas Unitarias")
class ServicioAdminPagosImplTest {

    @Mock
    private RepositorioPago repositorioPago;

    @Mock
    private RepositorioDetallePago repositorioDetallePago;

    @InjectMocks
    private ServicioAdminPagosImpl servicio;

    // ── Helper ────────────────────────────────────────────────────────────────
    private Pago crearPagoMock() {
        Pago pago = new Pago();
        pago.setId(UUID.randomUUID());
        pago.setUsuarioId(UUID.randomUUID());
        pago.setEstado(EstadoPago.COMPLETADO);
        return pago;
    }

    // =========================================================================
    // listarTodosLosPagos()
    // =========================================================================

    @Test
    @DisplayName("listarTodosLosPagos: debe retornar paginación con todos los pagos")
    void listarTodosLosPagos_debeRetornarPaginacion() {
        Pago p1 = crearPagoMock();
        Pago p2 = crearPagoMock();
        Page<Pago> pagina = new PageImpl<>(List.of(p1, p2));

        when(repositorioPago.findAll(any(Pageable.class))).thenReturn(pagina);

        Paginacion<Pago> resultado = servicio.listarTodosLosPagos(0, 10);

        assertThat(resultado).isNotNull();
        assertThat(resultado.contenido()).hasSize(2);
    }

    @Test
    @DisplayName("listarTodosLosPagos: cuando no hay pagos, debe retornar paginación vacía")
    void listarTodosLosPagos_sinPagos_debeRetornarVacio() {
        Page<Pago> paginaVacia = new PageImpl<>(List.of());

        when(repositorioPago.findAll(any(Pageable.class))).thenReturn(paginaVacia);

        Paginacion<Pago> resultado = servicio.listarTodosLosPagos(0, 10);

        assertThat(resultado.contenido()).isEmpty();
    }

    // =========================================================================
    // buscarPagoPorId()
    // =========================================================================

    @Test
    @DisplayName("buscarPagoPorId: cuando existe, debe retornar el pago")
    void buscarPagoPorId_cuandoExiste_debeRetornar() {
        UUID pagoId = UUID.randomUUID();
        Pago pago = crearPagoMock();
        pago.setId(pagoId);

        when(repositorioPago.findById(pagoId)).thenReturn(Optional.of(pago));

        Pago resultado = servicio.buscarPagoPorId(pagoId);

        assertThat(resultado).isNotNull();
        assertThat(resultado.getId()).isEqualTo(pagoId);
    }

    @Test
    @DisplayName("buscarPagoPorId: cuando no existe, debe lanzar IllegalArgumentException")
    void buscarPagoPorId_cuandoNoExiste_debeLanzarExcepcion() {
        UUID pagoId = UUID.randomUUID();
        when(repositorioPago.findById(pagoId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.buscarPagoPorId(pagoId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Pago no encontrado");
    }

    // =========================================================================
    // actualizarEstadoManual()
    // =========================================================================

    @Test
    @DisplayName("actualizarEstadoManual: con estado válido, debe actualizar el pago")
    void actualizarEstadoManual_conEstadoValido_debeActualizar() {
        UUID pagoId = UUID.randomUUID();
        Pago pago = crearPagoMock();
        pago.setId(pagoId);

        when(repositorioPago.findById(pagoId)).thenReturn(Optional.of(pago));
        when(repositorioPago.save(any(Pago.class))).thenReturn(pago);

        servicio.actualizarEstadoManual(pagoId, "REEMBOLSADO");

        assertThat(pago.getEstado()).isEqualTo(EstadoPago.REEMBOLSADO);
        verify(repositorioPago).save(pago);
    }

    @Test
    @DisplayName("actualizarEstadoManual: con estado inválido, debe lanzar IllegalArgumentException")
    void actualizarEstadoManual_conEstadoInvalido_debeLanzarExcepcion() {
        UUID pagoId = UUID.randomUUID();
        Pago pago = crearPagoMock();
        pago.setId(pagoId);

        when(repositorioPago.findById(pagoId)).thenReturn(Optional.of(pago));

        assertThatThrownBy(() -> servicio.actualizarEstadoManual(pagoId, "ESTADO_INVALIDO"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Estado no válido");
    }

    // =========================================================================
    // obtenerResumenGeneral()
    // =========================================================================

    @Test
    @DisplayName("obtenerResumenGeneral: debe retornar resumen con totales y agrupación por estado")
    void obtenerResumenGeneral_debeRetornarResumen() {
        when(repositorioPago.count()).thenReturn(50L);
        when(repositorioDetallePago.sumarIngresosTotales()).thenReturn(new BigDecimal("9999.99"));
        // Mockear conteos por estado
        for (EstadoPago estado : EstadoPago.values()) {
            when(repositorioPago.countByEstado(estado)).thenReturn(0L);
        }
        when(repositorioPago.contarSuscripcionesActivasPorPlan()).thenReturn(List.of());
        when(repositorioPago.sumarIngresosPorMesYAnio(any(Integer.class))).thenReturn(List.of());

        ResumenPagosDTO resumen = servicio.obtenerResumenGeneral(null);

        assertThat(resumen).isNotNull();
        assertThat(resumen.totalTransacciones()).isEqualTo(50L);
        assertThat(resumen.ingresosTotales()).isEqualByComparingTo("9999.99");
    }
}
