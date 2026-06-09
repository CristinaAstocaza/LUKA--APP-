package com.libreria.comun.respuesta;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.libreria.comun.enums.CodigoError;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Envoltura universal para todas las respuestas de la plataforma LUKA APP.
 * <p>
 * Esta clase garantiza un contrato Ãºnico entre el backend y los consumidores
 * (Frontend/IA), permitiendo manejar de forma consistente tanto respuestas
 * exitosas como errores. Utiliza anotaciones de Jackson para omitir campos
 * nulos.
 * </p>
 *
 * @param <T>         Tipo de dato que contiene la respuesta exitosa.
 * @param exito       Indica si la operaciÃ³n fue satisfactoria.
 * @param estado      CÃ³digo de estado HTTP (ej. 200, 201, 404, 500).
 * @param error       Etiqueta semÃ¡ntica del error (ej.
 *                    "USUARIO_NO_REGISTRADO").
 * @param mensaje     Mensaje descriptivo en espaÃ±ol para el usuario final.
 * @param datos       Carga Ãºtil de la respuesta (solo en caso de Ã©xito).
 * @param detalles    Lista de errores especÃ­ficos (principalmente para
 *                    validaciones).
 * @param ruta        URI del endpoint que originÃ³ la respuesta.
 * @param marcaTiempo Momento exacto en que se generÃ³ la respuesta.
 *
 * @version 1.1.0
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ResultadoApi<T>(
        boolean exito,
        int estado,
        String error,
        String mensaje,
        T datos,
        List<String> detalles,
        @SuppressWarnings("rawtypes") Paginacion pagina,
        String ruta,
        LocalDateTime marcaTiempo) {

    // =========================================================================
    // FÃBRICAS DE Ã‰XITO (HTTP 2xx)
    // =========================================================================
    /**
     * Crea una respuesta de Ã©xito estÃ¡ndar (HTTP 200 OK) con datos y mensaje.
     *
     * @param <T>     Tipo de dato.
     * @param datos   Carga Ãºtil de la respuesta.
     * @param mensaje DescripciÃ³n de la operaciÃ³n exitosa.
     * @param pagina  InformaciÃ³n de paginaciÃ³n si aplica.
     * @return Instancia de ResultadoApi parametrizada.
     */
    public static <T> ResultadoApi<T> exito(T datos, String mensaje, Paginacion<?> pagina) {
        return new ResultadoApi<>(true, 200, null, mensaje, datos, null, pagina, null, LocalDateTime.now());
    }

    /**
     * Crea una respuesta de Ã©xito estÃ¡ndar (HTTP 200 OK) con datos y mensaje.
     *
     * @param <T>     Tipo de dato.
     * @param datos   Carga Ãºtil de la respuesta.
     * @param mensaje DescripciÃ³n de la operaciÃ³n exitosa.
     * @return Instancia de ResultadoApi parametrizada.
     */
    public static <T> ResultadoApi<T> exito(T datos, String mensaje) {
        return exito(datos, mensaje, null);
    }

    /**
     * Crea una respuesta de Ã©xito estÃ¡ndar (HTTP 200 OK) solo con datos.
     *
     * @param <T>   Tipo de dato.
     * @param datos Carga Ãºtil de la respuesta.
     * @return Instancia de ResultadoApi parametrizada.
     */
    public static <T> ResultadoApi<T> exito(T datos) {
        return exito(datos, "OperaciÃ³n realizada con Ã©xito", null);
    }

    /**
     * Crea una respuesta de Ã©xito para creaciÃ³n de recursos (HTTP 201 Created).
     *
     * @param <T>     Tipo de dato.
     * @param datos   El recurso reciÃ©n creado.
     * @param mensaje ConfirmaciÃ³n de creaciÃ³n.
     * @return Instancia de ResultadoApi con estado 201.
     */
    public static <T> ResultadoApi<T> creado(T datos, String mensaje) {
        return new ResultadoApi<>(true, 201, null, mensaje, datos, null, null, null, LocalDateTime.now());
    }

    /**
     * Crea una respuesta de Ã©xito para solicitudes aceptadas pero no procesadas
     * aÃºn (HTTP 202 Accepted). Ãštil para tareas asÃ­ncronas o colas.
     *
     * @param mensaje Estado del proceso aceptado.
     * @return Instancia de ResultadoApi con estado 202.
     */
    public static ResultadoApi<Void> aceptado(String mensaje) {
        return new ResultadoApi<>(true, 202, null, mensaje, null, null, null, null, LocalDateTime.now());
    }

    /**
     * Crea una respuesta de Ã©xito sin contenido (HTTP 204 No Content).
     *
     * @param mensaje DescripciÃ³n de la operaciÃ³n (ej. eliminaciÃ³n exitosa).
     * @return Instancia de ResultadoApi con estado 204.
     */
    public static ResultadoApi<Void> sinContenido(String mensaje) {
        return new ResultadoApi<>(true, 204, null, mensaje, null, null, null, null, LocalDateTime.now());
    }

    // =========================================================================
    // FÃBRICAS DE ERROR BASADAS EN ENUM (RECOMENDADAS)
    // =========================================================================
    /**
     * Crea una falla utilizando el catÃ¡logo oficial de errores de LUKA APP.
     *
     * @param <T>     Tipo genÃ©rico (usualmente {@code Void}).
     * @param cod     Constante del Enum {@link CodigoError}.
     * @param mensaje Mensaje especÃ­fico del error.
     * @param ruta    URI solicitada.
     * @return Instancia de ResultadoApi parametrizada como error.
     */
    public static <T> ResultadoApi<T> falla(CodigoError cod, String mensaje, String ruta) {
        return new ResultadoApi<>(false, cod.getStatus().value(), cod.name(), mensaje, null, null, null, ruta,
                LocalDateTime.now());
    }

    /**
     * Crea una falla con lista de detalles especÃ­ficos utilizando el Enum
     * oficial. Ideal para errores de validaciÃ³n de negocio.
     *
     * @param <T>      Tipo genÃ©rico.
     * @param cod      Constante del Enum {@link CodigoError}.
     * @param mensaje  Mensaje general del error.
     * @param ruta     URI solicitada.
     * @param detalles Lista de strings con detalles tÃ©cnicos o de campo.
     * @return Instancia de ResultadoApi con lista de detalles poblada.
     */
    public static <T> ResultadoApi<T> fallaConDetalles(CodigoError cod, String mensaje, String ruta,
            List<String> detalles) {
        return new ResultadoApi<>(false, cod.getStatus().value(), cod.name(), mensaje, null, detalles, null, ruta,
                LocalDateTime.now());
    }

    // =========================================================================
    // FÃBRICAS DE ERROR GENÃ‰RICAS (FLEXIBILIDAD)
    // =========================================================================
    /**
     * Crea una respuesta de error estÃ¡ndar usando tipos primitivos.
     *
     * @param <T>     Tipo genÃ©rico.
     * @param estado  CÃ³digo de estado HTTP manual.
     * @param error   Etiqueta de error manual.
     * @param mensaje Mensaje descriptivo.
     * @param ruta    URI solicitada.
     * @return Instancia de ResultadoApi con exito=false.
     */
    public static <T> ResultadoApi<T> falla(int estado, String error, String mensaje, String ruta) {
        return new ResultadoApi<>(false, estado, error, mensaje, null, null, null, ruta, LocalDateTime.now());
    }

    /**
     * Crea una respuesta de error enriquecida con detalles usando tipos
     * primitivos.
     *
     * @param <T>      Tipo genÃ©rico.
     * @param estado   CÃ³digo de estado HTTP manual.
     * @param error    Etiqueta de error manual.
     * @param mensaje  Mensaje descriptivo.
     * @param ruta     URI solicitada.
     * @param detalles Lista de detalles.
     * @return Instancia de ResultadoApi con exito=false y detalles.
     */
    public static <T> ResultadoApi<T> fallaConDetalles(int estado, String error, String mensaje, String ruta,
            List<String> detalles) {
        return new ResultadoApi<>(false, estado, error, mensaje, null, detalles, null ,ruta, LocalDateTime.now());
    }
}
