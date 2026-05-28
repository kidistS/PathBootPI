package com.pathboot.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web MVC configuration: CORS settings.
 *
 * <p><b>PRODUCTION HARDENING:</b> {@code allowedOrigins("*")} is intentionally permissive
 * for local development and demo deployments. Before going to production, restrict this
 * to the actual frontend origin(s), for example:
 * <pre>
 *     .allowedOrigins("https://your-app.example.com")
 * </pre>
 * The matching CORS config in {@link SecurityConfig#corsConfigurationSource()} must be
 * updated at the same time — Spring Security processes preflight OPTIONS requests before
 * they reach the MVC layer, so both configs must agree.</p>
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins("*")   // ⚠ Restrict to specific origins in production
                .allowedMethods("GET", "POST", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .maxAge(3600);
    }
}

