package io.github.quizup.identity.domain.command;

import io.github.quizup.identity.domain.model.SocialProvider;
import io.github.quizup.identity.domain.model.SuspensionReinstatementReason;
import io.github.quizup.identity.domain.model.UserSuspensionReason;
import org.axonframework.modelling.command.TargetAggregateIdentifier;

import java.time.Instant;

public interface UserCommand {

    String userId();

    /**
     * Enregistre un utilisateur sans credential (passwordless / compte système).
     * L'authentification se fait ensuite par code à usage unique (OTP) ou provider social.
     */
    record RegisterUserCommand(
            @TargetAggregateIdentifier String userId,
            String email
    ) implements UserCommand {
    }

    record RegisterUserWithSocialCommand(
            @TargetAggregateIdentifier String userId,
            String email,
            SocialProvider provider
    ) implements UserCommand {
    }

    record LinkSocialProviderCommand(
            @TargetAggregateIdentifier String userId,
            SocialProvider provider
    ) implements UserCommand {
    }

    /**
     * Enregistre une connexion réussie d'un utilisateur existant.
     */
    record RecordUserLoginCommand(
            @TargetAggregateIdentifier String userId
    ) implements UserCommand {
    }

    /**
     * Supprime définitivement le compte : l'identité est purgée (email libéré, providers
     * détachés), le statut passe à {@code DELETED} et un événement transverse est publié pour
     * que chaque service supprime ou anonymise ses projections.
     */
    record DeleteAccountCommand(
            @TargetAggregateIdentifier String userId,
            String deletionId,
            Instant deletedAt
    ) implements UserCommand {
    }

    /**
     * Suspend un compte. {@code suspendedUntil} nul = suspension définitive.
     *
     * <p>La décision (motif, durée, acteur) est préparée par l'appelant : l'agrégat ne fait
     * aucun I/O et se contente de valider les invariants.</p>
     */
    record SuspendUserCommand(
            @TargetAggregateIdentifier String userId,
            UserSuspensionReason reason,
            String note,
            String requestedBy,
            Instant suspendedUntil,
            Instant suspendedAt
    ) implements UserCommand {
    }

    /**
     * Réintègre un compte suspendu. Idempotent : sans effet si le compte est déjà actif.
     */
    record ReinstateUserCommand(
            @TargetAggregateIdentifier String userId,
            SuspensionReinstatementReason reason,
            String note,
            String requestedBy,
            Instant reinstatedAt
    ) implements UserCommand {
    }
}
