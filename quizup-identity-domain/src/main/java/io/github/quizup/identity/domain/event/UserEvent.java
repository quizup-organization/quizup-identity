package io.github.quizup.identity.domain.event;

import io.github.quizup.identity.domain.model.SocialProvider;
import io.github.quizup.identity.domain.model.SuspensionReinstatementReason;
import io.github.quizup.identity.domain.model.UserSuspensionReason;

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

    /**
     * Événement émis à chaque connexion réussie (OTP ou sociale), y compris la première
     * connexion qui suit la création du compte.
     */
    record UserLoggedInEvent(
            String userId,
            Instant loggedInAt
    ) implements UserEvent {
    }

    /**
     * Événement émis lorsqu'un compte est suspendu.
     *
     * <p>L'email est porté par l'événement pour que les consommateurs (email, notification)
     * soient autosuffisants. {@code suspendedUntil} nul = suspension définitive.</p>
     */
    record UserSuspendedEvent(
            String userId,
            String email,
            String suspensionId,
            UserSuspensionReason reason,
            String note,
            String suspendedBy,
            Instant suspendedAt,
            Instant suspendedUntil
    ) implements UserEvent {
    }

    /**
     * Événement transverse émis lors de la suppression définitive d'un compte. Il ne porte
     * aucune donnée personnelle : uniquement l'identifiant technique et la clé d'audit.
     */
    record UserAccountDeletedEvent(
            String userId,
            String deletionId,
            Instant deletedAt
    ) implements UserEvent {
    }

    /**
     * Événement émis lorsqu'un compte est réintégré (manuellement ou par expiration).
     */
    record UserReinstatedEvent(
            String userId,
            String email,
            String suspensionId,
            SuspensionReinstatementReason reason,
            String note,
            String reinstatedBy,
            Instant reinstatedAt
    ) implements UserEvent {
    }
}
