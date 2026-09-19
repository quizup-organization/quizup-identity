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
            SocialProvider provider,
            Instant createdAt
    ) implements UserEvent {
    }

    /**
     * Événement émis lorsqu'un compte existant lie un nouveau provider social.
     */
    record SocialProviderLinkedEvent(
            String userId,
            SocialProvider provider,
            Instant linkedAt
    ) implements UserEvent {
    }
}
