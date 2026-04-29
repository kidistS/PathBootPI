package com.pathboot.controller;

import com.pathboot.config.ApiSecurityProperties;
import com.pathboot.model.request.AuthRequest;
import com.pathboot.model.response.AuthResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;
import java.util.Map;

/**
 * Authentication controller.
 *
 * <p>Call {@code POST /api/v1/auth/login} with your username and password.
 * On success the response contains the {@code apiKey} — copy it and paste it
 * into Swagger's <b>Authorize</b> dialog (or include it as the
 * {@code X-API-Key} header in every request).</p>
 *
 * <p>This endpoint is always public — no API key needed.</p>
 */
@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Authentication", description = "Login to obtain your API key for Swagger authorization")
public class AuthController {

    private static final Logger logger = LogManager.getLogger(AuthController.class);
    private static final URI CHAT_URI = URI.create("/api/v1/chat");

    private final ApiSecurityProperties securityProperties;

    public AuthController(ApiSecurityProperties securityProperties) {
        this.securityProperties = securityProperties;
    }

    @PostMapping(value = "/login",
                 consumes = MediaType.APPLICATION_JSON_VALUE,
                 produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
        summary = "Login",
        description = "Provide your username and password. "
                      + "On success the response contains your `apiKey` and a `Location` header "
                      + "pointing to `/api/v1/chat`.\n\n"
                      + "**Next step:** copy the `apiKey`, click the **Authorize 🔓** button above, "
                      + "paste it in, then go to the **Chat** section to start asking questions."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200",
                     description = "Login successful – copy apiKey, authorize, then call /api/v1/chat",
                     content = @Content(schema = @Schema(implementation = AuthResponse.class))),
        @ApiResponse(responseCode = "400", description = "Missing username or password"),
        @ApiResponse(responseCode = "401", description = "Invalid username or password")
    })
    public ResponseEntity<?> login(@Valid @RequestBody AuthRequest authRequest) {
        logger.info("Login attempt for user: {}", authRequest.getUsername());

        Map<String, String> users = securityProperties.getUsers();
        String storedPassword = users.get(authRequest.getUsername());

        // Constant-time comparison prevents timing-based username enumeration attacks.
        boolean passwordMatch = storedPassword != null &&
                MessageDigest.isEqual(
                        storedPassword.getBytes(StandardCharsets.UTF_8),
                        authRequest.getPassword().getBytes(StandardCharsets.UTF_8));

        if (!passwordMatch) {
            logger.warn("Login failed for user: {}", authRequest.getUsername());
            return ResponseEntity.status(401)
                    .body(Map.of("error", "Unauthorized",
                                 "message", "Invalid username or password."));
        }

        // Resolve a per-user key first; fall back to the first key in the global list.
        List<String> keys = securityProperties.getApiKeys();
        String apiKey = securityProperties.getUserKeys()
                .getOrDefault(authRequest.getUsername(),
                              keys.isEmpty() ? "" : keys.get(0));

        logger.info("Login successful for user: {} – redirecting to {}", authRequest.getUsername(), CHAT_URI);

        return ResponseEntity
                .ok()
                .location(CHAT_URI)              // Location: /api/v1/chat
                .header("X-API-Key", apiKey)     // convenience — key also in response header
                .body(AuthResponse.builder()
                        .apiKey(apiKey)
                        .instructions("Authentication successful! "
                                    + "1) Copy the apiKey above. "
                                    + "2) Click Authorize 🔓 in Swagger and paste it. "
                                    + "3) Go to the Chat section and call POST /api/v1/chat.")
                        .build());
    }
}
