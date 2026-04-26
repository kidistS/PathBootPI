package com.pathboot.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * JPA entity that persists the chat session state to SQLite.
 *
 * <p>Each row represents one user session. The full conversation history
 * is stored as a JSON string in {@code conversationHistoryJson} and
 * deserialized by {@link com.pathboot.session.UserSessionManager} on load.</p>
 *
 * <p>This allows sessions to survive application restarts, fulfilling the
 * Spring Session persistence requirement without requiring an external
 * session store (Redis / JDBC Spring Session tables).</p>
 */
@Entity
@Table(name = "user_sessions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class UserSessionEntity {

    /** UUID string that identifies the session, provided by the client or generated server-side. */
    @Id
    @Column(name = "session_id", nullable = false, unique = true)
    private String sessionId;

    /**
     * JSON-serialised snapshot of the {@link com.pathboot.session.SessionTurn} list.
     * Stored as TEXT in SQLite; deserialized by {@link com.pathboot.session.UserSessionManager}.
     */
    @Column(name = "conversation_history_json", columnDefinition = "TEXT")
    private String conversationHistoryJson;

    /** Number of turns recorded in this session (denormalized for quick lookup). */
    @Column(name = "turn_count", nullable = false)
    @Builder.Default
    private int turnCount = 0;

    /** Timestamp of the most recent update to this session. */
    @UpdateTimestamp
    @Column(name = "last_updated")
    private LocalDateTime lastUpdated;

    /** Timestamp when the session was first created. */
    @Column(name = "session_start", nullable = false, updatable = false)
    private LocalDateTime sessionStart;
}

