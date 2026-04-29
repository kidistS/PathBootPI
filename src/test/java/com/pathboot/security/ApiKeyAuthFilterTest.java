package com.pathboot.security;

import com.pathboot.util.PathBootConstants;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

@ExtendWith(MockitoExtension.class)
@DisplayName("ApiKeyAuthFilter – Unit Tests")
class ApiKeyAuthFilterTest {

    private static final String VALID_KEY   = "test-valid-key-001";
    private static final String INVALID_KEY = "wrong-key-999";

    private ApiKeyAuthFilter filter;

    @Mock
    private FilterChain filterChain;

    @BeforeEach
    void setUp() {
        filter = new ApiKeyAuthFilter(Set.of(VALID_KEY));
        // Clear security context between tests
        SecurityContextHolder.clearContext();
    }

    // ── Happy path ────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Valid API key")
    class ValidKey {

        @Test
        @DisplayName("request passes through to the next filter")
        void validKey_shouldPassThroughFilterChain() throws Exception {
            MockHttpServletRequest  request  = new MockHttpServletRequest();
            MockHttpServletResponse response = new MockHttpServletResponse();
            request.addHeader(PathBootConstants.API_KEY_HEADER, VALID_KEY);

            filter.doFilterInternal(request, response, filterChain);

            verify(filterChain, times(1)).doFilter(request, response);
        }

        @Test
        @DisplayName("response status is not set to 401")
        void validKey_responseShouldNotBe401() throws Exception {
            MockHttpServletRequest  request  = new MockHttpServletRequest();
            MockHttpServletResponse response = new MockHttpServletResponse();
            request.addHeader(PathBootConstants.API_KEY_HEADER, VALID_KEY);

            filter.doFilterInternal(request, response, filterChain);

            assertThat(response.getStatus()).isEqualTo(200); // default MockHttpServletResponse status
        }

        @Test
        @DisplayName("authentication is set in the SecurityContext")
        void validKey_shouldSetSecurityContextAuthentication() throws Exception {
            MockHttpServletRequest  request  = new MockHttpServletRequest();
            MockHttpServletResponse response = new MockHttpServletResponse();
            request.addHeader(PathBootConstants.API_KEY_HEADER, VALID_KEY);

            filter.doFilterInternal(request, response, filterChain);

            assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
            assertThat(SecurityContextHolder.getContext().getAuthentication().getPrincipal())
                    .isEqualTo(VALID_KEY);
        }
    }

    // ── Error cases ───────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Invalid or missing API key")
    class InvalidKey {

        @Test
        @DisplayName("missing key – filter chain is NOT called")
        void missingKey_shouldNotCallFilterChain() throws Exception {
            MockHttpServletRequest  request  = new MockHttpServletRequest();
            MockHttpServletResponse response = new MockHttpServletResponse();

            filter.doFilterInternal(request, response, filterChain);

            verify(filterChain, never()).doFilter(any(), any());
        }

        @Test
        @DisplayName("missing key – responds with 401")
        void missingKey_shouldReturn401() throws Exception {
            MockHttpServletRequest  request  = new MockHttpServletRequest();
            MockHttpServletResponse response = new MockHttpServletResponse();

            filter.doFilterInternal(request, response, filterChain);

            assertThat(response.getStatus()).isEqualTo(401);
        }

        @Test
        @DisplayName("invalid key – filter chain is NOT called")
        void wrongKey_shouldNotCallFilterChain() throws Exception {
            MockHttpServletRequest  request  = new MockHttpServletRequest();
            MockHttpServletResponse response = new MockHttpServletResponse();
            request.addHeader(PathBootConstants.API_KEY_HEADER, INVALID_KEY);

            filter.doFilterInternal(request, response, filterChain);

            verify(filterChain, never()).doFilter(any(), any());
        }

        @Test
        @DisplayName("invalid key – responds with 401")
        void wrongKey_shouldReturn401() throws Exception {
            MockHttpServletRequest  request  = new MockHttpServletRequest();
            MockHttpServletResponse response = new MockHttpServletResponse();
            request.addHeader(PathBootConstants.API_KEY_HEADER, INVALID_KEY);

            filter.doFilterInternal(request, response, filterChain);

            assertThat(response.getStatus()).isEqualTo(401);
        }

        @Test
        @DisplayName("invalid key – response body contains 'Unauthorized'")
        void wrongKey_responseBodyShouldContainUnauthorized() throws Exception {
            MockHttpServletRequest  request  = new MockHttpServletRequest();
            MockHttpServletResponse response = new MockHttpServletResponse();
            request.addHeader(PathBootConstants.API_KEY_HEADER, INVALID_KEY);

            filter.doFilterInternal(request, response, filterChain);

            assertThat(response.getContentAsString()).containsIgnoringCase("unauthorized");
        }
    }

    // ── shouldNotFilter ───────────────────────────────────────────────────────

    @Nested
    @DisplayName("Public paths skipped by shouldNotFilter")
    class PublicPaths {

        @ParameterizedTest(name = "[{index}] \"{0}\" is a public path → shouldNotFilter = true")
        @ValueSource(strings = {
            "/swagger-ui/index.html",
            "/swagger-ui",
            "/api-docs",
            "/v3/api-docs",
            "/v3/api-docs/swagger-config",
            "/webjars/swagger-ui/swagger-ui.js",
            "/api/v1/auth/login",
            "/api/v1/auth",
            "/actuator/health",
            "/actuator/info",
            "/error"
        })
        @DisplayName("public path is exempted from API-key check")
        void publicPaths_shouldBeExemptedFromFilter(String path) throws Exception {
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.setServletPath(path);

            assertThat(filter.shouldNotFilter(request)).isTrue();
        }

        @ParameterizedTest(name = "[{index}] \"{0}\" is a protected path → shouldNotFilter = false")
        @ValueSource(strings = {
            "/api/v1/chat",
            "/api/v1/chat/sessions/abc/history",
            "/api/v1/anything"
        })
        @DisplayName("protected API path is NOT exempted from filter")
        void protectedPaths_shouldNotBeExempted(String path) throws Exception {
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.setServletPath(path);

            assertThat(filter.shouldNotFilter(request)).isFalse();
        }
    }
}

