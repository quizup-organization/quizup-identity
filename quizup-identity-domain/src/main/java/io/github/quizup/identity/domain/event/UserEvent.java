package io.github.quizup.identity.domain.event;

import io.github.quizup.identity.domain.model.SocialProvider;

import java.time.Instant;

public interface UserEvent {
    String userId();

    /**
     * Événement émis lors de l'enregistrement d'un utilisateur.
     *
     * <p>Le compte ne porte plus de mot de passe : l'authentification repose sur un code
     * à usage unique (OTP email) ou sur un provider social. {@code provider} est non nul
     * uniquement pour un enregistrement social.</p>
     */
    record UserRegisteredEvent(
            String userId,
            String email,
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
