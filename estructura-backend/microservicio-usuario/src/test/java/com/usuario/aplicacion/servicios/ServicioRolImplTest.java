package com.usuario.aplicacion.servicios;

import com.usuario.dominio.entidades.Rol;
import com.usuario.dominio.entidades.Usuario;
import com.usuario.dominio.repositorios.RolRepository;
import com.usuario.dominio.repositorios.UsuarioRepository;
import com.usuario.infraestructura.mensajeria.PublicadorAuditoria;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Pruebas unitarias para {@link ServicioRolImpl}.
 * Verifica la lógica de sincronización de plan y roles de seguridad.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ServicioRolImpl — Pruebas Unitarias")
class ServicioRolImplTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private RolRepository rolRepository;

    @Mock
    private PublicadorAuditoria publicadorAuditoria;

    @InjectMocks
    private ServicioRolImpl servicio;

    // ── Helper ────────────────────────────────────────────────────────────────
    private Usuario crearUsuarioMock(UUID id) {
        Usuario u = new Usuario();
        u.setId(id);
        u.setCorreo("test@luka.com");
        u.setPlanActual("FREE");
        Set<Rol> roles = new HashSet<>();
        Rol rolFree = new Rol();
        rolFree.setNombre("ROLE_FREE");
        roles.add(rolFree);
        u.setRoles(roles);
        return u;
    }

    // =========================================================================
    // actualizarPlanUsuario()
    // =========================================================================

    @Test
    @DisplayName("actualizarPlanUsuario: debe actualizar el plan y reemplazar el rol correctamente")
    void actualizarPlanUsuario_cuandoUsuarioExiste_debeActualizarPlanYRol() {
        UUID usuarioId = UUID.randomUUID();
        Usuario usuario = crearUsuarioMock(usuarioId);
        Rol rolPremium = new Rol();
        rolPremium.setNombre("ROLE_PREMIUM");

        when(usuarioRepository.findById(usuarioId)).thenReturn(Optional.of(usuario));
        when(rolRepository.findByNombre("ROLE_PREMIUM")).thenReturn(Optional.of(rolPremium));
        when(usuarioRepository.save(any(Usuario.class))).thenReturn(usuario);

        servicio.actualizarPlanUsuario(usuarioId, "PREMIUM", LocalDateTime.now().plusMonths(1));

        assertThat(usuario.getPlanActual()).isEqualTo("PREMIUM");
        assertThat(usuario.getRoles()).contains(rolPremium);
        // El rol FREE debe haberse eliminado
        assertThat(usuario.getRoles()).noneMatch(r -> r.getNombre().equals("ROLE_FREE"));
        verify(usuarioRepository).save(usuario);
        verify(publicadorAuditoria).publicarTransaccion(any(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("actualizarPlanUsuario: cuando usuario no existe, debe lanzar RuntimeException")
    void actualizarPlanUsuario_cuandoUsuarioNoExiste_debeLanzarExcepcion() {
        UUID usuarioId = UUID.randomUUID();
        when(usuarioRepository.findById(usuarioId)).thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                servicio.actualizarPlanUsuario(usuarioId, "PREMIUM", LocalDateTime.now().plusMonths(1))
        ).isInstanceOf(RuntimeException.class)
         .hasMessageContaining("Usuario no encontrado");
    }

    @Test
    @DisplayName("actualizarPlanUsuario: cuando el rol no existe en BD, debe lanzar RuntimeException")
    void actualizarPlanUsuario_cuandoRolNoExiste_debeLanzarExcepcion() {
        UUID usuarioId = UUID.randomUUID();
        Usuario usuario = crearUsuarioMock(usuarioId);

        when(usuarioRepository.findById(usuarioId)).thenReturn(Optional.of(usuario));
        when(rolRepository.findByNombre("ROLE_PRO")).thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                servicio.actualizarPlanUsuario(usuarioId, "PRO", LocalDateTime.now().plusMonths(1))
        ).isInstanceOf(RuntimeException.class)
         .hasMessageContaining("ROLE_PRO");
    }
}
