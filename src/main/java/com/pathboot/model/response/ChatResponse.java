package com.pathboot.model.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Response body returned by the chat endpoint.
 *
 * <p>The {@code responseText} is always in the same language as the original user input.</p>
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
@Schema(description = "Response payload containing the generated answer")
public class ChatResponse {

    @Schema(description = "Generated answer in the user's original language")
    private String responseText;

    @Schema(description = "Language detected in the user's original input", example = "ENGLISH")
    private String detectedLanguage;

    @Schema(description = "Domain classified for the question", example = "TAX")
    private String detectedDomain;

    @Schema(
        description = "Session ID – include this in future requests to continue the conversation",
        example = "550e8400-e29b-41d4-a716-446655440000"
    )
    private String sessionId;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    @Schema(description = "Timestamp of the response")
    private LocalDateTime timestamp;
}

