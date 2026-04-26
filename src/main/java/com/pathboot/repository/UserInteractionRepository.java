package com.pathboot.repository;

import com.pathboot.model.UserInteraction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for persisting and querying {@link UserInteraction} records.
 */
@Repository
public interface UserInteractionRepository extends JpaRepository<UserInteraction, Long> {
}
