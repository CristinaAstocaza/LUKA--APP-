package com.auditoria.infraestructura.configuracion;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Propiedades de configuraciÃ³n para las polÃ­ticas de seguridad y defensa perimetral.
 * <p>
 * Reemplaza el uso disperso de {@code @Value} por un modelo tipado e inyectable.
 * </p>
 * 
 * @version 1.0.0
 */
@Component
@ConfigurationProperties(prefix = "auditoria.seguridad")
@Getter
@Setter
public class PropiedadesSeguridad {

    /**
     * NÃºmero mÃ¡ximo de intentos fallidos antes de bloquear la IP.
     */
    private int maxIntentosFallidos = 3;

    /**
     * Ventana de tiempo (en minutos) para contar los intentos fallidos.
     */
    private long ventanaMinutos = 10;

    /**
     * DuraciÃ³n del bloqueo automÃ¡tico (en minutos).
     */
    private long bloqueoMinutos = 60;
}
