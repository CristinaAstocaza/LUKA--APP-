package com.nucleo.financiero.infraestructura.configuracion;

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
 * ConfiguraciÃ³n de Seguridad para el NÃºcleo Financiero.
 * Extiende de {@link ConfiguracionSeguridadBase} para heredar la lÃ³gica de
 * autenticaciÃ³n JWT.
 * Define reglas de autorizaciÃ³n especÃ­ficas para este microservicio.
 *
 * @version 1.1.0
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class ConfiguracionSeguridad extends ConfiguracionSeguridadBase {

    public ConfiguracionSeguridad(FiltroJwt filtroJwt, PuntoEntradaJwt puntoEntradaJwt) {
        super(filtroJwt, puntoEntradaJwt);
    }

    /**
     * Configura la cadena de filtros de seguridad especÃ­fica para el microservicio
     * de nÃºcleo financiero.
     * 
     * @param http ConfiguraciÃ³n de seguridad
     * @return SecurityFilterChain configurado
     * @throws Exception Si ocurre un error en la configuraciÃ³n
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        // 1. Configuramos la base (JWT, Stateless, Exception handling)
        super.configurarAutorizacion(http);

        // 2. Definimos las reglas de este microservicio (De lo mÃ¡s especÃ­fico a lo
        // general)
        http.authorizeHttpRequests(auth -> auth
                // Endpoints de negocio financiero
                .requestMatchers("/api/v1/financiero/categorias/**")
                .hasAnyRole("FREE", "PREMIUM", "PRO", "ADMIN", "ADMINISTRADOR")
                .requestMatchers("/api/v1/transacciones/**").hasAnyRole("FREE", "PREMIUM", "PRO")
                .requestMatchers("/api/v1/ia/**").hasAnyRole("FREE", "PREMIUM", "PRO", "ADMIN", "ADMINISTRADOR")

                // Monitoreo y DocumentaciÃ³n (PÃºblico)
                .requestMatchers("/actuator/**", "/error/**").permitAll()
                .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()

                // 3. BLOQUEO TOTAL AL FINAL
                .anyRequest().authenticated());

        return http.build();
    }
}
