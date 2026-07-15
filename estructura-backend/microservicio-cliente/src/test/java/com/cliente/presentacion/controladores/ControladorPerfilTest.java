package com.cliente.presentacion.controladores;

import com.cliente.aplicacion.dtos.respuestas.RespuestaDatosPersonales;
import com.cliente.aplicacion.puertos.ServicioDatosPersonales;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Prueba de capa Web para {@link ControladorPerfil}.
 * Usa {@link WebMvcTest} que levanta solo la capa MVC (sin Base de Datos ni
 * seguridad real).
 */
@WebMvcTest(ControladorPerfil.class)
@DisplayName("ControladorPerfil — Pruebas MockMvc")
class ControladorPerfilTest {

        @Autowired
        private MockMvc mockMvc;

        @MockitoBean
        private ServicioDatosPersonales servicioDatosPersonales;

        // ── Datos de prueba ───────────────────────────────────────────────────────
        private static final UUID USUARIO_ID = UUID.randomUUID();

        private RespuestaDatosPersonales crearRespuestaMock() {
                return new RespuestaDatosPersonales(
                                "12345678", "Ana", "García", "FEMENINO",
                                25, "999888777", null, "Perú", "Lima", true);
        }

        // =========================================================================
        // GET /api/v1/clientes/perfil/{usuarioId}
        // =========================================================================

        @Test
        @WithMockUser
        @DisplayName("GET /perfil/{id}: cuando existe, debe retornar 200 con datos del perfil")
        void consultar_cuandoExiste_debeRetornar200() throws Exception {
                when(servicioDatosPersonales.consultar(any(), any()))
                                .thenReturn(crearRespuestaMock());

                mockMvc.perform(get("/api/v1/clientes/perfil/{usuarioId}", USUARIO_ID)
                                .contentType(MediaType.APPLICATION_JSON))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.datos.nombres").value("Ana"))
                                .andExpect(jsonPath("$.datos.apellidos").value("García"))
                                .andExpect(jsonPath("$.datos.datosCompletos").value(true));
        }

        // =========================================================================
        // PUT /api/v1/clientes/perfil/{usuarioId}
        // =========================================================================

        @Test
        @WithMockUser
        @DisplayName("PUT /perfil/{id}: con body válido, debe retornar 200 con datos actualizados")
        void actualizar_conBodyValido_debeRetornar200() throws Exception {
                String body = """
                                {
                                    "nombres": "Ana",
                                    "apellidos": "García",
                                    "dni": "12345678",
                                    "telefono": "999888777",
                                    "ciudad": "Lima",
                                    "pais": "Perú",
                                    "genero": "FEMENINO",
                                    "edad": 25
                                }
                                """;

                when(servicioDatosPersonales.actualizar(any(), any(), any(), any()))
                                .thenReturn(crearRespuestaMock());

                mockMvc.perform(put("/api/v1/clientes/perfil/{usuarioId}", USUARIO_ID)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body)
                                .with(csrf()))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.datos.nombres").value("Ana"));
        }

        @Test
        @WithMockUser
        @DisplayName("PUT /perfil/{id}: con body vacío, debe retornar 400 o procesar sin errores de parsing")
        void actualizar_conBodyVacio_debePermitirParcial() throws Exception {
                String body = "{}";

                when(servicioDatosPersonales.actualizar(any(), any(), any(), any()))
                                .thenReturn(crearRespuestaMock());

                mockMvc.perform(put("/api/v1/clientes/perfil/{usuarioId}", USUARIO_ID)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body)
                                .with(csrf()))
                                .andExpect(status().isOk());
        }
}
