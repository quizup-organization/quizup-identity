package io.github.quizup.identity.domain.event;

import io.github.quizup.identity.domain.model.SocialProvider;

import java.time.Instant;

public interface UserEvent {
    String userId();

    /**
     * Événement émis lors de l'enregistrement d'un utilisateur.
     */
    record UserRegisteredEvent(
            String userId,
            String email,
            String password,
            String name,
            SocialProvider provider,
            Instant createdAt
    ) implements UserEvent {
    }
}
