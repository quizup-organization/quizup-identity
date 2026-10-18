package io.github.quizup.identity.domain.model;

import java.time.Instant;

/**
 * Dernière suspension connue d'un compte.
 *
 * <p>Les métadonnées sont conservées après réintégration afin de garder une trace lisible
 * dans le read model. Le statut effectif reste porté par {@link AccountStatus}.</p>
 */
public record UserSuspension(
        String suspensionId,
        UserSuspensionReason reason,
        String note,
        String suspendedBy,
        Instant suspendedAt,
        Instant suspendedUntil,
        Instant reinstatedAt,
        String reinstatedBy,
        SuspensionReinstatementReason reinstatementReason,
        String reinstatementNote
) {

    /** Vrai si la suspension est définitive (aucune échéance). */
    public boolean permanent() {
        return suspendedUntil == null;
    }

    public UserSuspension reinstated(Instant at,
                                     String by,
                                     SuspensionReinstatementReason why,
                                     String reinstatementNote) {
        return new UserSuspension(
                suspensionId,
                reason,
                note,
                suspendedBy,
                suspendedAt,
                suspendedUntil,
                at,
                by,
                why,
                reinstatementNote);
    }
}
