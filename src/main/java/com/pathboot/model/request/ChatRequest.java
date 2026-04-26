package com.pathboot.model.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

/**
 * Incoming request body for the chat endpoint.
 *
 * <p>The {@code sessionId} is optional on the first request; the server will
 * generate one and return it so subsequent calls can continue the same session.</p>
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
@Schema(description = "Request payload for submitting a user question")
public class ChatRequest {

    @NotBlank(message = "userInput must not be blank")
    @Size(max = 4000, message = "userInput must not exceed 4000 characters")
    @Schema(
        description = "User question in English, Amharic, or Norwegian",
        example = "How much income tax do I pay in Norway?"
    )
    private String userInput;

    @Schema(
        description = "Optional session ID to continue an existing conversation",
        example = "550e8400-e29b-41d4-a716-446655440000"
    )
    private String sessionId;
}

