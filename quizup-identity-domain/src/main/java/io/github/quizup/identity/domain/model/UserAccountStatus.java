package io.github.quizup.identity.domain.model;

import java.time.Instant;

/**
 * Vue légère du statut d'un compte, utilisée par les filtres et les badges publics.
 */
public record UserAccountStatus(
        String userId,
        AccountStatus status,
        Instant suspendedUntil
) {

    public boolean suspended(Instant now) {
        return AccountStatusRules.isSuspended(status, suspendedUntil, now);
    }

    public boolean deleted() {
        return status == AccountStatus.DELETED;
    }
}
