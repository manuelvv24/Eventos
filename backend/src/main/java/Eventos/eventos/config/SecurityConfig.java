package Eventos.eventos.config;

import Eventos.eventos.util.RolNames;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
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

@Configuration
public class SecurityConfig {

    @Autowired
    private JwtFilter jwtFilter;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .csrf(csrf -> csrf.disable())
            .formLogin(form -> form.disable())
            .httpBasic(basic -> basic.disable())
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint((request, response, authException) -> {
                    response.setStatus(jakarta.servlet.http.HttpServletResponse.SC_UNAUTHORIZED);
                    response.setContentType("application/json");
                    response.getWriter().write("{\"error\":\"No autorizado. Token inválido o expirado.\"}");
                })
            )
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                .requestMatchers("/error").permitAll()
                .requestMatchers("/api/auth/**").permitAll()
                .requestMatchers("/api/public/**").permitAll()
                .requestMatchers("/api/test/**").permitAll()
                .requestMatchers("/api/restaurar", "/api/restaurar/**").hasRole(RolNames.SUPER_ADMIN)
                .requestMatchers(HttpMethod.GET, "/api/eventos/papelera").hasRole(RolNames.SUPER_ADMIN)
                .requestMatchers(HttpMethod.PUT, "/api/eventos/{id}/restaurar").hasRole(RolNames.SUPER_ADMIN)
                .requestMatchers(HttpMethod.GET, "/api/eventos").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/eventos/{id}").permitAll()
                .requestMatchers("/api/dashboard/**").hasAnyRole(RolNames.ADMIN, RolNames.SUPER_ADMIN, RolNames.MONITOR)
                .requestMatchers("/api/auditoria", "/api/auditoria/**").hasAnyRole(RolNames.ADMIN, RolNames.SUPER_ADMIN)
                .requestMatchers("/api/usuarios", "/api/usuarios/**").hasAnyRole(RolNames.ADMIN, RolNames.SUPER_ADMIN)
                .requestMatchers("/api/roles", "/api/roles/**").hasAnyRole(RolNames.ADMIN, RolNames.SUPER_ADMIN)
                .requestMatchers("/api/importacion", "/api/importacion/**").hasAnyRole(RolNames.ADMIN, RolNames.SUPER_ADMIN, RolNames.OPERADOR)
                .requestMatchers("/api/check-in", "/api/check-in/**").hasAnyRole(RolNames.ADMIN, RolNames.SUPER_ADMIN, RolNames.OPERADOR)
                .requestMatchers(HttpMethod.POST, "/api/eventos", "/api/eventos/**").hasAnyRole(RolNames.ADMIN, RolNames.SUPER_ADMIN, RolNames.OPERADOR)
                .requestMatchers(HttpMethod.PUT, "/api/eventos", "/api/eventos/**").hasAnyRole(RolNames.ADMIN, RolNames.SUPER_ADMIN, RolNames.OPERADOR)
                .requestMatchers(HttpMethod.DELETE, "/api/eventos", "/api/eventos/**").hasAnyRole(RolNames.ADMIN, RolNames.SUPER_ADMIN)
                .requestMatchers(HttpMethod.GET, "/api/configuracion-certificado", "/api/configuracion-certificado/**").authenticated()
                .requestMatchers(HttpMethod.POST, "/api/configuracion-certificado/**").hasAnyRole(RolNames.ADMIN, RolNames.SUPER_ADMIN)
                .requestMatchers(HttpMethod.PUT, "/api/configuracion-certificado/**").hasAnyRole(RolNames.ADMIN, RolNames.SUPER_ADMIN)
                .requestMatchers(HttpMethod.DELETE, "/api/configuracion-certificado/**").hasAnyRole(RolNames.ADMIN, RolNames.SUPER_ADMIN)
                .requestMatchers(HttpMethod.PATCH, "/api/configuracion-certificado/**").hasAnyRole(RolNames.ADMIN, RolNames.SUPER_ADMIN)
                .requestMatchers(HttpMethod.POST, "/api/certificados/regenerar/**").hasAnyRole(RolNames.ADMIN, RolNames.SUPER_ADMIN)
                .requestMatchers("/api/certificados", "/api/certificados/**").authenticated()
                .requestMatchers("/api/asistencias", "/api/asistencias/**").authenticated()
                .requestMatchers("/api/participantes", "/api/participantes/**").authenticated()
                .requestMatchers("/api/invitado", "/api/invitado/**").authenticated()
                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        // Orígenes exactos (desarrollo local y red local)
        configuration.setAllowedOrigins(List.of(
            "http://localhost:5173",
            "http://localhost:5174",
            "http://localhost:3000",
            "http://localhost:4173",
            "http://10.1.163.54:5173",
            "http://10.1.163.54:5174",
            "http://10.78.138.153:5173",
            "http://10.78.138.153:5174"
        ));

        // Patrones: cubre cualquier puerto de Vite en localhost y dominios Railway
        configuration.setAllowedOriginPatterns(List.of(
            "http://localhost:*",
            "http://10.1.163.*:*",
            "http://10.78.138.*:*",
            "https://*.up.railway.app",
            "https://*.railway.app"
        ));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);
        configuration.setExposedHeaders(List.of("Authorization"));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}