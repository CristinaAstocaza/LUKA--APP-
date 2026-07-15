package com.mensajeria.aplicacion.servicios;

import com.mensajeria.aplicacion.excepciones.LimiteIntentosExcedidoException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Pruebas unitarias de control de tasa de envío para el microservicio de mensajería.
 * Nombre del archivo solicitado por el usuario para fácil identificación: TEST_Mensajeria.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("TEST_Mensajeria — Pruebas Unitarias de Throttling")
class TEST_Mensajeria {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private ThrottlingServiceImpl throttlingService;

    // =========================================================================
    // registrarIntento()
    // =========================================================================

    @Test
    @DisplayName("registrarIntento: primer intento del día debe incrementar y fijar la expiración")
    void registrarIntento_primerIntento_debeFijarExpiracion() {
        String canal = "email";
        String identificador = "user@luka.com";
        String claveEsperada = "luka:throttling:email:user@luka.com";

        // Simulamos que Redis devuelve 1 al incrementar (primer intento)
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.increment(claveEsperada)).thenReturn(1L);

        // Ejecución (no debería lanzar excepción ya que 1 es menor al límite de 3)
        assertThatCode(() -> throttlingService.registrarIntento(canal, identificador))
                .doesNotThrowAnyException();

        // Verificaciones
        verify(valueOperations).increment(claveEsperada);
        // Debe configurar la expiración en Redis usando expireAt para el primer intento
        verify(redisTemplate).expireAt(eq(claveEsperada), any(Instant.class));
    }

    @Test
    @DisplayName("registrarIntento: intentos subsiguientes (2 y 3) no deben reconfigurar la expiración")
    void registrarIntento_intentosPermitidos_noDebeReconfigurarExpiracion() {
        String canal = "sms";
        String identificador = "+51987654321";
        String claveEsperada = "luka:throttling:sms:+51987654321";

        // Simulamos que es el segundo intento del día (retorna 2)
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.increment(claveEsperada)).thenReturn(2L);

        // Ejecución
        assertThatCode(() -> throttlingService.registrarIntento(canal, identificador))
                .doesNotThrowAnyException();

        verify(valueOperations).increment(claveEsperada);
        // NO debe configurar la expiración si no es el primer intento
        verify(redisTemplate, never()).expireAt(anyString(), any(Instant.class));
    }

    @Test
    @DisplayName("registrarIntento: al superar el límite de 3 intentos, debe lanzar LimiteIntentosExcedidoException")
    void registrarIntento_superaLimite_debeLanzarExcepcion() {
        String canal = "email";
        String identificador = "spam@luka.com";
        String claveEsperada = "luka:throttling:email:spam@luka.com";

        // Simulamos que se supera el límite de 3 (retorna 4)
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.increment(claveEsperada)).thenReturn(4L);

        // Ejecución & Verificación de Excepción
        assertThatThrownBy(() -> throttlingService.registrarIntento(canal, identificador))
                .isInstanceOf(LimiteIntentosExcedidoException.class);

        verify(valueOperations).increment(claveEsperada);
    }
}
