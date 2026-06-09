package com.mensajeria.aplicacion.puertos;

/**
 * Contrato del servicio de envÃ­o de SMS.
 * <p>
 * Desacopla la implementaciÃ³n de Twilio del dominio, permitiendo mock en tests
 * y futura migraciÃ³n de proveedor sin impacto en el resto del servicio.
 * </p>
 *
 * @version 1.1.0
 */
public interface ISmsService {

    /**
     * EnvÃ­a un cÃ³digo OTP de 6 dÃ­gitos al nÃºmero de telÃ©fono indicado mediante
     * un proveedor de SMS externo (actualmente Twilio).
     *
     * @param telefono NÃºmero destino en formato E.164 (ej. {@code +51987654321}).
     *                 Debe haber sido previamente validado por
     *                 {@link #esNumeroValido(String)}.
     * @param codigo   CÃ³digo OTP de 6 dÃ­gitos que el usuario debe ingresar.
     * @throws RuntimeException si el proveedor externo rechaza el envÃ­o o no estÃ¡
     *                          disponible.
     */
    void enviarCodigoVerificacion(String telefono, String codigo);

    /**
     * Valida que el nÃºmero de telÃ©fono cumple el formato E.164 requerido por el
     * proveedor SMS.
     *
     * @param telefono NÃºmero de telÃ©fono a validar (con o sin cÃ³digo de paÃ­s).
     * @return {@code true} si el nÃºmero tiene formato E.164 vÃ¡lido (ej.
     *         {@code +51987654321}), {@code false} en caso contrario.
     */
    boolean esNumeroValido(String telefono);
}
