package com.libreria.comun.seguridad;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import java.util.List;

/**
 * ConfiguraciÃ³n base de seguridad para el ecosistema LUKA APP.
 * <p>
 * Proporciona los cimientos de una arquitectura Stateless: 1. Deshabilita CSRF
 * y sesiones (Stateless). 2. Configura el Punto de Entrada para errores 401 en
 * JSON. 3. Inyecta el Filtro JWT centralizado. 4. Define rutas de
 * infraestructura comunes (Swagger, Actuator). 5. Habilita CORS para permitir
 * peticiones desde el frontend.
 * </p>
 *
 */
@RequiredArgsConstructor
public abstract class ConfiguracionSeguridadBase {

    protected final FiltroJwt filtroJwt;
    protected final PuntoEntradaJwt puntoEntradaJwt;

    @org.springframework.beans.factory.annotation.Autowired
    protected FiltroAutenticacionInterna filtroAutenticacionInterna;

    /**
     * Define la configuraciÃ³n comÃºn de la cadena de filtros. Los microservicios
     * deben llamar a este mÃ©todo y aÃ±adir sus rutas especÃ­ficas.
     *
     * @param http ConfiguraciÃ³n de seguridad de Spring.
     * @return {@link SecurityFilterChain} configurado.
     * @throws Exception Si ocurre un error en la configuraciÃ³n.
     */
    protected HttpSecurity configurarAutorizacion(HttpSecurity http) throws Exception {
        return http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(csrf -> csrf.disable())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(ex -> ex.authenticationEntryPoint(puntoEntradaJwt))
                .addFilterBefore(filtroJwt, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(filtroAutenticacionInterna, FiltroJwt.class);
    }

    /**
     * ConfiguraciÃ³n de CORS permitiendo orÃ­genes de desarrollo.
     * @return 
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        // Permitir orÃ­genes de desarrollo comunes
        configuration.setAllowedOrigins(List.of(
                "http://localhost:8081",
                "http://localhost:8082",
                "http://localhost:8083",
                "http://localhost:8084",
                "http://localhost:8085",
                "http://localhost:8086",
                "http://localhost:61274",
                "http://localhost:4200",
                "http://localhost:4201",
                "http://localhost:61878", // Puerto dinÃ¡mico detectado
                "http://localhost:5173" // Vite
        ));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type", "X-Requested-With", "Accept", "Origin", "Access-Control-Request-Method", "Access-Control-Request-Headers"));
        configuration.setExposedHeaders(List.of("Authorization"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    /**
     * Bean de codificaciÃ³n de contraseÃ±as Ãºnico para toda la plataforma. Se usa
     * BCrypt con fuerza 12 para mÃ¡xima seguridad.
     *
     * @return password
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(10);
    }
}
