package com.auditoria.aplicacion.servicios;

import com.auditoria.aplicacion.dtos.RespuestaVerificacionIpDTO;
import com.auditoria.aplicacion.excepciones.IpBloqueadaException;
import com.auditoria.dominio.entidades.ListaNegraIp;
import com.auditoria.dominio.repositorios.AuditoriaAccesoRepository;
import com.auditoria.dominio.repositorios.ListaNegraIpRepository;
import com.auditoria.infraestructura.configuracion.PropiedadesSeguridad;
import com.libreria.comun.enums.EstadoEvento;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Pruebas unitarias para {@link ServicioSeguridadAuditoriaImpl}.
 * Verifica la lógica de detección de fuerza bruta y bloqueo de IPs.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ServicioSeguridadAuditoriaImpl — Pruebas Unitarias")
class ServicioSeguridadAuditoriaImplTest {

    @Mock
    private AuditoriaAccesoRepository repositorioAcceso;

    @Mock
    private ListaNegraIpRepository repositorioListaNegra;

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private PropiedadesSeguridad propiedadesSeguridad;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    @InjectMocks
    private ServicioSeguridadAuditoriaImpl servicio;

    // =========================================================================
    // verificarIntentoFallido()
    // =========================================================================

    @Test
    @DisplayName("verificarIntentoFallido: cuando fallos < umbral, NO debe bloquear la IP")
    void verificarIntentoFallido_bajoUmbral_noDebeBloquear() {
        String ip = "10.0.0.1";
        when(propiedadesSeguridad.getVentanaMinutos()).thenReturn(10L);
        when(propiedadesSeguridad.getMaxIntentosFallidos()).thenReturn(5);
        when(repositorioAcceso.contarIntentosPorIpYEstadoDesde(
                eq(ip), eq(EstadoEvento.FALLO), any(LocalDateTime.class)))
                .thenReturn(3L); // solo 3, umbral es 5

        // No debe lanzar excepción
        assertThatCode(() -> servicio.verificarIntentoFallido(ip))
                .doesNotThrowAnyException();

        verify(repositorioListaNegra, never()).save(any());
    }

    @Test
    @DisplayName("verificarIntentoFallido: cuando fallos >= umbral, debe bloquear la IP y lanzar excepción")
    void verificarIntentoFallido_sobreUmbral_debeBloquearYLanzar() {
        String ip = "10.0.0.2";
        when(propiedadesSeguridad.getVentanaMinutos()).thenReturn(10L);
        when(propiedadesSeguridad.getMaxIntentosFallidos()).thenReturn(3);
        when(propiedadesSeguridad.getBloqueoMinutos()).thenReturn(60L);
        when(repositorioAcceso.contarIntentosPorIpYEstadoDesde(
                eq(ip), eq(EstadoEvento.FALLO), any(LocalDateTime.class)))
                .thenReturn(5L); // 5 > umbral de 3

        when(repositorioListaNegra.findActivaByIp(eq(ip), any(LocalDateTime.class)))
                .thenReturn(Optional.empty());
        when(repositorioListaNegra.save(any(ListaNegraIp.class)))
                .thenReturn(new ListaNegraIp());
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        assertThatThrownBy(() -> servicio.verificarIntentoFallido(ip))
                .isInstanceOf(IpBloqueadaException.class);

        verify(repositorioListaNegra).save(any(ListaNegraIp.class));
        verify(valueOperations).set(anyString(), eq(true), any(java.time.Duration.class));
    }

    // =========================================================================
    // verificarEstadoIp()
    // =========================================================================

    @Test
    @DisplayName("verificarEstadoIp: cuando IP está bloqueada, debe retornar estado 'bloqueada'")
    void verificarEstadoIp_cuandoBloqueada_debeRetornarBloqueada() {
        String ip = "192.168.1.100";
        ListaNegraIp bloqueada = new ListaNegraIp();
        bloqueada.setIp(ip);
        bloqueada.setMotivo("Múltiples intentos fallidos");
        bloqueada.setFechaExpiracion(LocalDateTime.now().plusHours(1));

        when(repositorioListaNegra.findActivaByIp(eq(ip), any(LocalDateTime.class)))
                .thenReturn(Optional.of(bloqueada));

        RespuestaVerificacionIpDTO resultado = servicio.verificarEstadoIp(ip);

        assertThat(resultado.bloqueada()).isTrue();
        assertThat(resultado.ip()).isEqualTo(ip);
    }

    @Test
    @DisplayName("verificarEstadoIp: cuando IP está libre, debe retornar estado 'libre'")
    void verificarEstadoIp_cuandoLibre_debeRetornarLibre() {
        String ip = "192.168.1.200";

        when(repositorioListaNegra.findActivaByIp(eq(ip), any(LocalDateTime.class)))
                .thenReturn(Optional.empty());

        RespuestaVerificacionIpDTO resultado = servicio.verificarEstadoIp(ip);

        assertThat(resultado.bloqueada()).isFalse();
    }

    // =========================================================================
    // limpiarBloqueosExpirados()
    // =========================================================================

    @Test
    @DisplayName("limpiarBloqueosExpirados: debe eliminar bloqueos vencidos del repositorio")
    void limpiarBloqueosExpirados_debeEliminarExpirados() {
        when(repositorioListaNegra.eliminarBloqueoExpirados(any(LocalDateTime.class))).thenReturn(3);

        servicio.limpiarBloqueosExpirados();

        verify(repositorioListaNegra).eliminarBloqueoExpirados(any(LocalDateTime.class));
    }
}
