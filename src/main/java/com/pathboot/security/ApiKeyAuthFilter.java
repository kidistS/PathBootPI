package com.pathboot.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pathboot.util.PathBootConstants;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Servlet filter that enforces API key authentication.
 *
 * <p>Public paths (Swagger UI, OpenAPI docs, webjars, auth login, actuator)
 * are automatically skipped via {@link #shouldNotFilter} — those paths never
 * reach the key-check logic regardless of what the caller sends.</p>
 */
public class ApiKeyAuthFilter extends OncePerRequestFilter {

    private static final Logger logger = LogManager.getLogger(ApiKeyAuthFilter.class);

    private final Set<String> validApiKeys;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public ApiKeyAuthFilter(Set<String> validApiKeys) {
        this.validApiKeys = validApiKeys;
    }

    // ── Skip the filter entirely for public paths ─────────────────────────────

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getServletPath();
        return path.startsWith("/swagger-ui")
            || path.startsWith("/api-docs")
            || path.startsWith("/v3/api-docs")
            || path.startsWith("/webjars")
            || path.startsWith("/api/v1/auth")
            || path.startsWith("/actuator/health")
            || path.startsWith("/actuator/info")
            || path.equals("/error");
    }

    // ── Key validation for all other paths ───────────────────────────────────

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String apiKey = request.getHeader(PathBootConstants.API_KEY_HEADER);

        if (apiKey == null || !validApiKeys.contains(apiKey)) {
            logger.warn("Unauthorized request – missing or invalid '{}' header from {}",
                    PathBootConstants.API_KEY_HEADER, request.getRemoteAddr());

            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            objectMapper.writeValue(response.getWriter(), Map.of(
                    "error", "Unauthorized",
                    "message", "Missing or invalid API key. "
                               + "Provide a valid key in the '"
                               + PathBootConstants.API_KEY_HEADER + "' header."
            ));
            return;
        }

        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                apiKey, null, List.of(new SimpleGrantedAuthority("ROLE_API_USER")));
        SecurityContextHolder.getContext().setAuthentication(auth);

        filterChain.doFilter(request, response);
    }
}
