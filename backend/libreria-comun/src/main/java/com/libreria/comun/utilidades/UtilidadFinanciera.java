package com.libreria.comun.utilidades;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Utilidad transversal para el formateo consistente de datos financieros y
 * temporales.
 * 
 */
public final class UtilidadFinanciera {

    @SuppressWarnings("deprecation")
    private static final DateTimeFormatter FORMATO_ES = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss",
            new Locale("es", "PE"));

    private UtilidadFinanciera() {
    }

    /**
     * Formatea una fecha al estÃ¡ndar de lectura en espaÃ±ol para la plataforma.
     * 
     * @param fecha LocalDateTime a formatear.
     * @return String formateado (ej: 08/05/2026 14:30:00).
     */
    public static String formatearFecha(LocalDateTime fecha) {
        return (fecha == null) ? "" : fecha.format(FORMATO_ES);
    }

    /**
     * Asegura que un monto financiero tenga exactamente 2 decimales con redondeo
     * hacia arriba.
     * Evita errores de precisiÃ³n en cÃ¡lculos de saldos.
     * 
     * @param monto El valor numÃ©rico a normalizar.
     * @return BigDecimal con escala 2.
     */
    public static BigDecimal normalizarMonto(BigDecimal monto) {
        if (monto == null)
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        return monto.setScale(2, RoundingMode.HALF_UP);
    }
}
