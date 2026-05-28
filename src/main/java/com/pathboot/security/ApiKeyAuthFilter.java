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
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Servlet filter that enforces API key authentication.
 *
 * <p>Public paths (Swagger UI, OpenAPI docs, webjars, auth login, actuator)
 * are automatically skipped via {@link #shouldNotFilter} — those paths never
 * reach the key-check logic regardless of what the caller sends.</p>
 *
 * <h3>Timing-safe key validation</h3>
 * <p>API key comparison uses {@link MessageDigest#isEqual} for every candidate key,
 * without short-circuiting on the first match. This prevents a timing side-channel
 * that would otherwise allow an attacker to determine whether a guessed key matches
 * any entry in the valid-key set by measuring response latency.</p>
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

        if (!isValidApiKey(apiKey)) {
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

    // ── Private helpers ──────────────────────────────────────────────────────

    /**
     * Constant-time API key validation.
     *
     * <p>Iterates ALL valid keys without short-circuiting on the first match so that
     * the response time does not reveal how many keys were tested before a match
     * (preventing a timing oracle).</p>
     *
     * @param providedKey the key extracted from the request header (may be null)
     * @return {@code true} if the key matches any entry in {@code validApiKeys}
     */
    private boolean isValidApiKey(String providedKey) {
        if (providedKey == null) return false;
        byte[] provided = providedKey.getBytes(StandardCharsets.UTF_8);
        boolean match = false;
        for (String validKey : validApiKeys) {
            // Do NOT return early — always check all keys to keep timing uniform.
            if (MessageDigest.isEqual(validKey.getBytes(StandardCharsets.UTF_8), provided)) {
                match = true;
            }
        }
        return match;
    }
}
