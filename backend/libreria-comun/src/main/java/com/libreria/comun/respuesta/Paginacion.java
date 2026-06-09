package com.libreria.comun.respuesta;

import java.util.List;
import org.springframework.data.domain.Page;

/**
 * Envoltura estandarizada para respuestas paginadas de LUKA APP.
 * <p>
 * Transforma el objeto {@link org.springframework.data.domain.Page} de Spring Data
 * en un formato simplificado y consistente para el consumo desde el Frontend.
 * </p>
 * 
 * @param <T>            Tipo de los elementos contenidos en la pÃ¡gina.
 * @param contenido      Lista de elementos de la pÃ¡gina actual.
 * @param numeroPagina   Ãndice de la pÃ¡gina actual (basado en cero).
 * @param tamaÃ±oPagina   Cantidad de elementos solicitados por pÃ¡gina.
 * @param totalElementos Cantidad total de registros existentes en la base de datos.
 * @param totalPaginas   Cantidad total de pÃ¡ginas disponibles.
 * @param esUltima       Indica si la pÃ¡gina actual es la Ãºltima de la colecciÃ³n.
 * 
 */
public record Paginacion<T>(
    List<T> contenido,
    int numeroPagina,
    int tamaÃ±oPagina,
    long totalElementos,
    int totalPaginas,
    boolean esUltima
) {
    /**
     * Convierte una instancia de {@code Page} de Spring Data a nuestro formato {@code Pagina}.
     * 
     * @param <T>  Tipo de dato.
     * @param page Objeto de paginaciÃ³n de Spring Data.
     * @return Una nueva instancia de {@code Pagina} con los metadatos mapeados.
     */
    public static <T> Paginacion<T> desde(Page<T> page) {
        return new Paginacion<>(
            page.getContent(),
            page.getNumber(),
            page.getSize(),
            page.getTotalElements(),
            page.getTotalPages(),
            page.isLast()
        );
    }
}
