package com.pathboot.model.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Login request for {@code POST /api/v1/auth/login}.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Credentials to obtain an API key")
public class AuthRequest {

    @NotBlank(message = "Username must not be blank")
    @Schema(description = "Your username", example = "admin")
    private String username;

    @NotBlank(message = "Password must not be blank")
    @Schema(description = "Your password", example = "pathboot-admin-2026")
    private String password;
}

