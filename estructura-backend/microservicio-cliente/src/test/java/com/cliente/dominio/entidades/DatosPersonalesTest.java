package com.cliente.dominio.entidades;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pruebas unitarias para la entidad {@link DatosPersonales}.
 * No requiere Spring Context — lógica pura de dominio.
 */
@DisplayName("DatosPersonales — Pruebas de Entidad")
class DatosPersonalesTest {

    // =========================================================================
    // evaluarDatosCompletos()
    // =========================================================================

    @Test
    @DisplayName("evaluarDatosCompletos: debe retornar true cuando todos los campos mínimos están presentes")
    void evaluarDatosCompletos_cuandoCamposCompletos_debeRetornarTrue() {
        DatosPersonales datos = DatosPersonales.builder()
                .nombres("Juan")
                .apellidos("Pérez")
                .dni("12345678")
                .telefono("987654321")
                .ciudad("Lima")
                .build();

        assertThat(datos.evaluarDatosCompletos()).isTrue();
    }

    @Test
    @DisplayName("evaluarDatosCompletos: debe retornar false cuando falta el DNI")
    void evaluarDatosCompletos_cuandoFaltaDni_debeRetornarFalse() {
        DatosPersonales datos = DatosPersonales.builder()
                .nombres("Juan")
                .apellidos("Pérez")
                .telefono("987654321")
                .ciudad("Lima")
                .build();

        assertThat(datos.evaluarDatosCompletos()).isFalse();
    }

    @Test
    @DisplayName("evaluarDatosCompletos: debe retornar false cuando falta el teléfono")
    void evaluarDatosCompletos_cuandoFaltaTelefono_debeRetornarFalse() {
        DatosPersonales datos = DatosPersonales.builder()
                .nombres("Ana")
                .apellidos("García")
                .dni("87654321")
                .ciudad("Arequipa")
                .build();

        assertThat(datos.evaluarDatosCompletos()).isFalse();
    }

    @Test
    @DisplayName("evaluarDatosCompletos: debe retornar false cuando nombres está en blanco")
    void evaluarDatosCompletos_cuandoNombresBlanco_debeRetornarFalse() {
        DatosPersonales datos = DatosPersonales.builder()
                .nombres("   ")
                .apellidos("López")
                .dni("11223344")
                .telefono("999888777")
                .ciudad("Cusco")
                .build();

        assertThat(datos.evaluarDatosCompletos()).isFalse();
    }

    @Test
    @DisplayName("evaluarDatosCompletos: debe retornar false cuando todos los campos son null")
    void evaluarDatosCompletos_cuandoTodoNull_debeRetornarFalse() {
        DatosPersonales datos = DatosPersonales.builder().build();

        assertThat(datos.evaluarDatosCompletos()).isFalse();
    }

    // =========================================================================
    // obtenerNombreCompleto()
    // =========================================================================

    @Test
    @DisplayName("obtenerNombreCompleto: debe retornar 'nombre apellido' concatenados")
    void obtenerNombreCompleto_conNombresYApellidos_debeRetornarConcatenado() {
        DatosPersonales datos = DatosPersonales.builder()
                .nombres("María")
                .apellidos("Torres")
                .build();

        assertThat(datos.obtenerNombreCompleto()).isEqualTo("María Torres");
    }

    @Test
    @DisplayName("obtenerNombreCompleto: cuando nombres es null debe retornar solo apellido")
    void obtenerNombreCompleto_cuandoNombresNull_debeRetornarApellido() {
        DatosPersonales datos = DatosPersonales.builder()
                .apellidos("Ramírez")
                .build();

        assertThat(datos.obtenerNombreCompleto()).isEqualTo("Ramírez");
    }

    @Test
    @DisplayName("obtenerNombreCompleto: cuando apellidos es null debe retornar solo nombre")
    void obtenerNombreCompleto_cuandoApellidosNull_debeRetornarNombre() {
        DatosPersonales datos = DatosPersonales.builder()
                .nombres("Carlos")
                .build();

        assertThat(datos.obtenerNombreCompleto()).isEqualTo("Carlos");
    }

    @Test
    @DisplayName("obtenerNombreCompleto: cuando ambos son null debe retornar cadena vacía")
    void obtenerNombreCompleto_cuandoAmbosNull_debeRetornarVacio() {
        DatosPersonales datos = DatosPersonales.builder().build();

        assertThat(datos.obtenerNombreCompleto()).isEmpty();
    }
}
