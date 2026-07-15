package com.usuario.aplicacion.servicios;

import com.libreria.comun.respuesta.Paginacion;
import com.usuario.dominio.entidades.Usuario;
import com.usuario.dominio.repositorios.UsuarioRepository;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Pruebas unitarias para {@link ServicioAdminUsuarioImpl}.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ServicioAdminUsuarioImpl — Pruebas Unitarias")
class ServicioAdminUsuarioImplTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @InjectMocks
    private ServicioAdminUsuarioImpl servicio;

    // ── Helper ────────────────────────────────────────────────────────────────
    private Usuario crearUsuarioMock() {
        Usuario u = new Usuario();
        u.setId(UUID.randomUUID());
        u.setCorreo("user@luka.com");
        u.setHabilitado(true);
        return u;
    }

    // =========================================================================
    // buscarUsuarios()
    // =========================================================================

    @Test
    @DisplayName("buscarUsuarios: sin filtros, debe retornar todos los usuarios paginados")
    @SuppressWarnings("unchecked")
    void buscarUsuarios_sinFiltros_debeRetornarTodos() {
        Usuario u1 = crearUsuarioMock();
        Usuario u2 = crearUsuarioMock();
        Page<Usuario> pagina = new PageImpl<>(List.of(u1, u2));

        when(usuarioRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(pagina);

        Paginacion<Usuario> resultado = servicio.buscarUsuarios(
                null, null, null, null, null, 0, 10
        );

        assertThat(resultado).isNotNull();
        assertThat(resultado.contenido()).hasSize(2);
    }

    @Test
    @DisplayName("buscarUsuarios: filtrando por habilitado=true, debe retornar solo activos")
    @SuppressWarnings("unchecked")
    void buscarUsuarios_filtrandoHabilitados_debeRetornarActivos() {
        Usuario u1 = crearUsuarioMock();
        Page<Usuario> pagina = new PageImpl<>(List.of(u1));

        when(usuarioRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(pagina);

        Paginacion<Usuario> resultado = servicio.buscarUsuarios(
                true, null, null, null, null, 0, 10
        );

        assertThat(resultado.contenido()).hasSize(1);
        assertThat(resultado.contenido().get(0).isEnabled()).isTrue();
    }

    @Test
    @DisplayName("buscarUsuarios: cuando no hay resultados, debe retornar paginación vacía")
    @SuppressWarnings("unchecked")
    void buscarUsuarios_sinResultados_debeRetornarVacio() {
        Page<Usuario> paginaVacia = new PageImpl<>(List.of());

        when(usuarioRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(paginaVacia);

        Paginacion<Usuario> resultado = servicio.buscarUsuarios(
                null, null, "textoInexistente", null, null, 0, 10
        );

        assertThat(resultado.contenido()).isEmpty();
    }

    @Test
    @DisplayName("buscarUsuarios: debe delegar la construcción de paginación al repositorio")
    @SuppressWarnings("unchecked")
    void buscarUsuarios_debeUsarPaginacionCorrectamente() {
        Page<Usuario> pagina = new PageImpl<>(List.of());

        when(usuarioRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(pagina);

        servicio.buscarUsuarios(null, null, null, null, null, 2, 5);

        // Verificar que sí se llamó al repositorio con paginación
        verify(usuarioRepository).findAll(any(Specification.class), any(Pageable.class));
    }
}
