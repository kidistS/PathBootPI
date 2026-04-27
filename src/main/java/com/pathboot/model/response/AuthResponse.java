package com.pathboot.model.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Returned by {@code POST /api/v1/auth/login} on successful authentication.
 * Copy the {@code apiKey} value and paste it into Swagger's Authorize dialog.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Login response containing the API key")
public class AuthResponse {

    @Schema(description = "API key to use in the X-API-Key header", example = "pathboot-dev-key-2026")
    private String apiKey;

    @Schema(description = "How to use the key", example = "Add X-API-Key header to every request")
    private String instructions;
}

