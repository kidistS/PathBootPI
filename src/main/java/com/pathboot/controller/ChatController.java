package com.pathboot.controller;

import com.pathboot.model.request.ChatRequest;
import com.pathboot.model.response.ChatResponse;
import com.pathboot.service.ChatFacadeService;
import com.pathboot.session.SessionTurn;
import com.pathboot.session.UserSessionManager;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for the chat Q&A API.
 *
 * <p>All endpoints are under {@code /api/v1/chat}.</p>
 *
 * <p>Implements the <em>Facade</em> pattern from the controller perspective –
 * it delegates all logic to {@link ChatFacadeService}.</p>
 */
@RestController
@RequestMapping("/api/v1/chat")
@Tag(name = "Chat", description = "Multilingual domain Q&A endpoints (Tax, NAV, Immigration)")
@SecurityRequirement(name = "ApiKeyAuth")
public class ChatController {

    private static final Logger logger = LogManager.getLogger(ChatController.class);

    private final ChatFacadeService chatFacadeService;
    private final UserSessionManager userSessionManager;

    public ChatController(ChatFacadeService chatFacadeService,
                          UserSessionManager userSessionManager) {
        this.chatFacadeService  = chatFacadeService;
        this.userSessionManager = userSessionManager;
    }

    // ─── POST /api/v1/chat ────────────────────────────────────────────────────

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE,
                 produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
        summary = "Submit a question",
        description = "Accepts a question in English, Amharic, or Norwegian and returns an answer " +
                      "in the same language. The domain (Tax, NAV, Immigration) is auto-detected."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Answer generated successfully",
                     content = @Content(schema = @Schema(implementation = ChatResponse.class))),
        @ApiResponse(responseCode = "400", description = "Invalid request payload"),
        @ApiResponse(responseCode = "401", description = "Missing or invalid X-API-Key header"),
        @ApiResponse(responseCode = "503", description = "AI/translation service unavailable")
    })
    public ResponseEntity<ChatResponse> submitQuestion(@Valid @RequestBody ChatRequest chatRequest) {
        logger.info("POST /api/v1/chat – session: {}", chatRequest.getSessionId());
        ChatResponse chatResponse = chatFacadeService.processUserChatRequest(chatRequest);
        return ResponseEntity.ok(chatResponse);
    }

    // ─── GET /api/v1/chat/sessions/{sessionId}/history ───────────────────────

    @GetMapping("/sessions/{sessionId}/history")
    @Operation(
        summary = "Get session conversation history",
        description = "Returns all conversation turns for the given session ID."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "History returned (may be empty)"),
        @ApiResponse(responseCode = "404", description = "Session not found (empty history returned)")
    })
    public ResponseEntity<List<SessionTurn>> getSessionHistory(
            @Parameter(description = "Session ID returned by the chat endpoint")
            @PathVariable String sessionId) {

        logger.info("GET /api/v1/chat/sessions/{}/history", sessionId);
        List<SessionTurn> history = userSessionManager.getSessionHistory(sessionId);
        return ResponseEntity.ok(history);
    }

    // ─── DELETE /api/v1/chat/sessions/{sessionId} ────────────────────────────

    @DeleteMapping("/sessions/{sessionId}")
    @Operation(
        summary = "Clear a session",
        description = "Removes all in-memory conversation history for the specified session."
    )
    @ApiResponse(responseCode = "204", description = "Session cleared")
    public ResponseEntity<Void> clearSession(
            @Parameter(description = "Session ID to clear")
            @PathVariable String sessionId) {

        logger.info("DELETE /api/v1/chat/sessions/{}", sessionId);
        userSessionManager.clearSession(sessionId);
        return ResponseEntity.noContent().build();
    }
}

