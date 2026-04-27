package com.pathboot.config;

import com.pathboot.security.ApiKeyAuthFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.HashSet;
import java.util.List;

/**
 * Spring Security configuration for PathBoot PI.
 *
 * <p><b>Authentication model:</b> API key sent in the {@code X-API-Key} request header.
 * The session is fully stateless — no cookies, no JSESSIONID.</p>
 *
 * <p>Set {@code api.security.enabled=false} in {@code application.yml} to disable
 * enforcement (useful for local development without a key).</p>
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final ApiSecurityProperties securityProperties;

    public SecurityConfig(ApiSecurityProperties securityProperties) {
        this.securityProperties = securityProperties;
    }

    /** Paths that are always accessible without an API key. */
    private static final String[] PUBLIC_PATHS = {
            // Swagger UI pages & static assets
            "/swagger-ui/**",
            "/swagger-ui.html",
            "/swagger-ui/index.html",
            // Springdoc OpenAPI JSON
            "/api-docs/**",
            "/api-docs.yaml",
            "/v3/api-docs/**",
            // Webjars (Swagger UI CSS/JS bundles)
            "/webjars/**",
            // Spring error page (must be public or auth errors become infinite loops)
            "/error",
            // Actuator
            "/actuator/health",
            "/actuator/info",
            // Login (no key needed to obtain a key)
            "/api/v1/auth/**"
    };

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> {
                    auth.requestMatchers(PUBLIC_PATHS).permitAll();
                    if (securityProperties.isEnabled()) {
                        auth.anyRequest().authenticated();
                    } else {
                        auth.anyRequest().permitAll();
                    }
                });

        if (securityProperties.isEnabled()) {
            http.addFilterBefore(
                    new ApiKeyAuthFilter(new HashSet<>(securityProperties.getApiKeys())),
                    UsernamePasswordAuthenticationFilter.class);
        }

        return http.build();
    }

    /**
     * CORS — mirrors the settings in {@link WebConfig} so Spring Security
     * does not block pre-flight OPTIONS requests before they reach the MVC layer.
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOriginPatterns(List.of("*"));
        config.setAllowedMethods(List.of("GET", "POST", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
