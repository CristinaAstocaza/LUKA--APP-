package com.nucleo.financiero.aplicacion.servicios;

import com.libreria.comun.excepciones.ExcepcionConflicto;
import com.libreria.comun.excepciones.ExcepcionRecursoNoEncontrado;
import com.nucleo.financiero.aplicacion.dtos.respuestas.CategoriaDTO;
import com.nucleo.financiero.aplicacion.dtos.solicitudes.CategoriaRequestDTO;
import com.nucleo.financiero.aplicacion.mappers.CategoriaMapper;
import com.nucleo.financiero.dominio.entidades.Categoria;
import com.nucleo.financiero.dominio.entidades.Categoria.TipoMovimiento;
import com.nucleo.financiero.dominio.repositorios.CategoriaRepository;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;

import static org.mockito.Mockito.*;

/**
 * Pruebas unitarias para {@link CategoriaServiceImpl}.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("CategoriaServiceImpl — Pruebas Unitarias")
class CategoriaServiceImplTest {

    @Mock
    private CategoriaRepository categoriaRepository;

    @Mock
    private CategoriaMapper categoriaMapper;

    @InjectMocks
    private CategoriaServiceImpl servicio;

    // ── Helpers ────────────────────────────────────────────────────────────────
    private Categoria crearCategoriaMock(String nombre) {
        return Categoria.builder()
                .id(UUID.randomUUID())
                .nombre(nombre)
                .tipo(TipoMovimiento.GASTO)
                .build();
    }

    private CategoriaDTO crearDtoMock(String nombre) {
        return new CategoriaDTO(UUID.randomUUID(), nombre, null, null, TipoMovimiento.GASTO.name());
    }

    // =========================================================================
    // crear()
    // =========================================================================

    @Test
    @DisplayName("crear: con nombre único, debe persistir y retornar DTO")
    void crear_conNombreUnico_debePersistirYRetornar() {
        CategoriaRequestDTO request = new CategoriaRequestDTO(
                "Alimentación", "Gastos de comida", "🍔", TipoMovimiento.GASTO);
        Categoria guardada = crearCategoriaMock("Alimentación");
        CategoriaDTO dtoEsperado = crearDtoMock("Alimentación");

        when(categoriaRepository.existsByNombreIgnoreCase("Alimentación")).thenReturn(false);
        when(categoriaRepository.save(any(Categoria.class))).thenReturn(guardada);
        when(categoriaMapper.aDto(guardada)).thenReturn(dtoEsperado);

        CategoriaDTO resultado = servicio.crear(request);

        assertThat(resultado).isNotNull();
        assertThat(resultado.nombre()).isEqualTo("Alimentación");
        verify(categoriaRepository).save(any(Categoria.class));
    }

    @Test
    @DisplayName("crear: con nombre duplicado, debe lanzar ExcepcionConflicto")
    void crear_conNombreDuplicado_debeLanzarConflicto() {
        CategoriaRequestDTO request = new CategoriaRequestDTO(
                "Transporte", "Gastos de movilidad", "🚗", TipoMovimiento.GASTO);

        when(categoriaRepository.existsByNombreIgnoreCase("Transporte")).thenReturn(true);

        assertThatThrownBy(() -> servicio.crear(request))
                .isInstanceOf(ExcepcionConflicto.class);

        verify(categoriaRepository, never()).save(any());
    }

    // =========================================================================
    // listarTodas() y listarPorTipo()
    // =========================================================================

    @Test
    @DisplayName("listarTodas: debe retornar todas las categorías mapeadas a DTO")
    void listarTodas_debeRetornarTodasMapeadas() {
        Categoria c1 = crearCategoriaMock("Alimentación");
        Categoria c2 = crearCategoriaMock("Transporte");
        CategoriaDTO dto1 = crearDtoMock("Alimentación");
        CategoriaDTO dto2 = crearDtoMock("Transporte");

        when(categoriaRepository.findAll()).thenReturn(List.of(c1, c2));
        when(categoriaMapper.aDto(c1)).thenReturn(dto1);
        when(categoriaMapper.aDto(c2)).thenReturn(dto2);

        List<CategoriaDTO> resultado = servicio.listarTodas();

        assertThat(resultado).hasSize(2);
    }

    @Test
    @DisplayName("listarPorTipo: debe retornar solo categorías del tipo especificado")
    void listarPorTipo_debeRetornarSoloDelTipo() {
        Categoria c1 = crearCategoriaMock("Salario");
        c1.setTipo(TipoMovimiento.INGRESO);

        when(categoriaRepository.findByTipo(TipoMovimiento.INGRESO)).thenReturn(List.of(c1));
        when(categoriaMapper.aDto(c1)).thenReturn(crearDtoMock("Salario"));

        List<CategoriaDTO> resultado = servicio.listarPorTipo(TipoMovimiento.INGRESO);

        assertThat(resultado).hasSize(1);
    }

    // =========================================================================
    // obtenerPorId()
    // =========================================================================

    @Test
    @DisplayName("obtenerPorId: cuando existe, debe retornar el DTO de la categoría")
    void obtenerPorId_cuandoExiste_debeRetornar() {
        UUID id = UUID.randomUUID();
        Categoria categoria = crearCategoriaMock("Salud");

        when(categoriaRepository.findById(id)).thenReturn(Optional.of(categoria));
        when(categoriaMapper.aDto(categoria)).thenReturn(crearDtoMock("Salud"));

        CategoriaDTO resultado = servicio.obtenerPorId(id);

        assertThat(resultado).isNotNull();
    }

    @Test
    @DisplayName("obtenerPorId: cuando no existe, debe lanzar ExcepcionRecursoNoEncontrado")
    void obtenerPorId_cuandoNoExiste_debeLanzarExcepcion() {
        UUID id = UUID.randomUUID();
        when(categoriaRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.obtenerPorId(id))
                .isInstanceOf(ExcepcionRecursoNoEncontrado.class);
    }

    // =========================================================================
    // eliminar()
    // =========================================================================

    @Test
    @DisplayName("eliminar: cuando existe, debe eliminar correctamente")
    void eliminar_cuandoExiste_debeEliminar() {
        UUID id = UUID.randomUUID();
        when(categoriaRepository.existsById(id)).thenReturn(true);

        servicio.eliminar(id);

        verify(categoriaRepository).deleteById(id);
    }

    @Test
    @DisplayName("eliminar: cuando no existe, debe lanzar ExcepcionRecursoNoEncontrado")
    void eliminar_cuandoNoExiste_debeLanzarExcepcion() {
        UUID id = UUID.randomUUID();
        when(categoriaRepository.existsById(id)).thenReturn(false);

        assertThatThrownBy(() -> servicio.eliminar(id))
                .isInstanceOf(ExcepcionRecursoNoEncontrado.class);

        verify(categoriaRepository, never()).deleteById(any());
    }
}
