package com.pathboot.repository;

import com.pathboot.model.UserSessionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Spring Data JPA repository for persisting and retrieving {@link UserSessionEntity} records.
 *
 * <p>Backed by the SQLite database, giving user sessions persistence across restarts.</p>
 */
@Repository
public interface UserSessionRepository extends JpaRepository<UserSessionEntity, String> {

    /**
     * Finds an active session by its unique session ID.
     *
     * @param sessionId the UUID session identifier
     * @return the session entity, or empty if not found
     */
    Optional<UserSessionEntity> findBySessionId(String sessionId);
}
