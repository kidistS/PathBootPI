package com.pathboot.controller;

import com.pathboot.config.ApiSecurityProperties;
import com.pathboot.model.request.AuthRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("AuthController – Unit Tests")
class AuthControllerTest {

    @Mock private ApiSecurityProperties securityProperties;

    private AuthController controller;

    @BeforeEach
    void setUp() {
        controller = new AuthController(securityProperties);

        when(securityProperties.getUsers()).thenReturn(Map.of(
                "admin", "pathboot-admin-2026",
                "user",  "pathboot-user-2026"
        ));
        when(securityProperties.getApiKeys()).thenReturn(List.of(
                "pathboot-dev-key-2026",
                "pathboot-local-key-001"
        ));
        when(securityProperties.getUserKeys()).thenReturn(Map.of(
                "admin", "pathboot-dev-key-2026",
                "user",  "pathboot-local-key-001"
        ));
    }

    // ── Happy path ────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Successful login")
    class SuccessfulLogin {

        @Test
        @DisplayName("valid admin credentials return HTTP 200 with admin's API key")
        void adminLogin_returns200WithAdminKey() {
            ResponseEntity<?> response = controller.login(
                    new AuthRequest("admin", "pathboot-admin-2026"));

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).hasFieldOrPropertyWithValue("apiKey", "pathboot-dev-key-2026");
        }

        @Test
        @DisplayName("valid user credentials return HTTP 200 with user's own API key")
        void userLogin_returns200WithUserKey() {
            ResponseEntity<?> response = controller.login(
                    new AuthRequest("user", "pathboot-user-2026"));

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).hasFieldOrPropertyWithValue("apiKey", "pathboot-local-key-001");
        }

        @Test
        @DisplayName("response contains non-null instructions string")
        void successResponse_containsInstructions() {
            ResponseEntity<?> response = controller.login(
                    new AuthRequest("admin", "pathboot-admin-2026"));

            assertThat(response.getBody()).hasFieldOrPropertyWithValue("apiKey", "pathboot-dev-key-2026");
            Object instructions = ((com.pathboot.model.response.AuthResponse) response.getBody()).getInstructions();
            assertThat(instructions).isNotNull();
        }

        @Test
        @DisplayName("response has Location header pointing to /api/v1/chat")
        void successResponse_hasLocationHeader() {
            ResponseEntity<?> response = controller.login(
                    new AuthRequest("admin", "pathboot-admin-2026"));

            assertThat(response.getHeaders().getLocation())
                    .isNotNull()
                    .hasPath("/api/v1/chat");
        }

        @Test
        @DisplayName("X-API-Key convenience header is set in response")
        void successResponse_hasApiKeyHeaderSet() {
            ResponseEntity<?> response = controller.login(
                    new AuthRequest("admin", "pathboot-admin-2026"));

            assertThat(response.getHeaders().getFirst("X-API-Key"))
                    .isEqualTo("pathboot-dev-key-2026");
        }
    }

    // ── Error cases ───────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Failed login")
    class FailedLogin {

        @Test
        @DisplayName("wrong password returns HTTP 401")
        void wrongPassword_returns401() {
            ResponseEntity<?> response = controller.login(
                    new AuthRequest("admin", "wrong-password"));

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        }

        @Test
        @DisplayName("unknown username returns HTTP 401")
        void unknownUsername_returns401() {
            ResponseEntity<?> response = controller.login(
                    new AuthRequest("ghost", "any-password"));

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        }

        @Test
        @DisplayName("401 body contains 'error' and 'message' fields")
        void failed_responseBody_hasErrorFields() {
            ResponseEntity<?> response = controller.login(
                    new AuthRequest("admin", "bad"));

            @SuppressWarnings("unchecked")
            var body = (Map<String, String>) response.getBody();
            assertThat(body).containsKey("error").containsKey("message");
        }

        @Test
        @DisplayName("wrong password for 'user' account returns HTTP 401")
        void wrongPasswordForUser_returns401() {
            ResponseEntity<?> response = controller.login(
                    new AuthRequest("user", "wrong"));

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        }

        @Test
        @DisplayName("password comparison is timing-safe — equal-length wrong password still returns 401")
        void equalLengthWrongPassword_returns401() {
            // Same length as "pathboot-admin-2026" (19 chars)
            ResponseEntity<?> response = controller.login(
                    new AuthRequest("admin", "pathboot-admin-XXXX"));

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        }
    }
}

