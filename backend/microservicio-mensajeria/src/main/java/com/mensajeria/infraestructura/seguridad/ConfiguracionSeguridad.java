package com.mensajeria.infraestructura.seguridad;

import com.libreria.comun.seguridad.ConfiguracionSeguridadBase;
import com.libreria.comun.seguridad.FiltroJwt;
import com.libreria.comun.seguridad.PuntoEntradaJwt;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

/**
 * ConfiguraciÃ³n de Spring Security para el microservicio de mensajerÃ­a.
 * <p>
 * Hereda de {@link ConfiguracionSeguridadBase} la configuraciÃ³n stateless
 * comÃºn (deshabilitar CSRF, sesiÃ³n sin estado, punto de entrada JWT y filtros
 * de infraestructura) y aÃ±ade Ãºnicamente las rutas pÃºblicas propias de este
 * microservicio (endpoints OTP).
 * </p>
 *
 * @version 1.1.0
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class ConfiguracionSeguridad extends ConfiguracionSeguridadBase {

    /**
     * Construye la configuraciÃ³n inyectando el filtro JWT y el punto de entrada
     * de la librerÃ­a comÃºn mediante inyecciÃ³n por constructor.
     *
     * @param filtroJwt       Filtro centralizado que valida el token JWT en cada
     *                        peticiÃ³n autenticada.
     * @param puntoEntradaJwt Manejador que devuelve HTTP 401 en JSON cuando no
     *                        hay token o es invÃ¡lido.
     */
    public ConfiguracionSeguridad(FiltroJwt filtroJwt, PuntoEntradaJwt puntoEntradaJwt) {
        super(filtroJwt, puntoEntradaJwt);
    }

    /**
     * Define la cadena de filtros de seguridad del microservicio.
     * <p>
     * Llama a {@code configurarAutorizacion} de la clase base para aplicar la
     * polÃ­tica stateless y luego permite de forma explÃ­cita los endpoints OTP,
     * que son pÃºblicos por diseÃ±o (el usuario aÃºn no estÃ¡ autenticado cuando
     * solicita o valida su OTP).
     * </p>
     * 
     * @param http Objeto de configuraciÃ³n de Spring Security.
     * @return {@link SecurityFilterChain} con las reglas de este microservicio.
     * @throws Exception si la configuraciÃ³n de Spring Security falla.
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        // 1. Configuramos la base (JWT, Stateless, Exception handling)
        super.configurarAutorizacion(http);

        // 2. Definimos las reglas de este microservicio (De lo mÃ¡s especÃ­fico a lo
        // general)
        http.authorizeHttpRequests(auth -> auth
                // Rutas pÃºblicas comunicacion interna
                .requestMatchers("/api/v1/auth/**").permitAll()
                .requestMatchers("/api/v1/datos-personales/**").permitAll()

                // Endpoints de OTP son pÃºblicos (se validan internamente por UUID/CÃ³digo)
                .requestMatchers("/api/v1/mensajeria/otp/**").permitAll()

                // Endpoints de administraciÃ³n requerirÃ¡n ADMIN
                .requestMatchers("/api/v1/mensajeria/admin/**").hasRole("ADMIN")

                // Monitoreo y DocumentaciÃ³n (PÃºblico)
                .requestMatchers("/actuator/**", "/error/**").permitAll()
                .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()

                // 3. BLOQUEO TOTAL AL FINAL
                .anyRequest().authenticated());

        return http.build();
    }
}
