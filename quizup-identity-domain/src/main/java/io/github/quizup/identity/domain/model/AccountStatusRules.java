package io.github.quizup.identity.domain.model;

import java.time.Instant;
import java.util.Objects;

/**
 * Règles pures du statut de compte : une suspension temporaire dont l'échéance est passée
 * est considérée active, même si la réintégration n'a pas encore été projetée.
 */
public interface AccountStatusRules {

    static boolean isSuspended(AccountStatus status, Instant suspendedUntil, Instant now) {
        if (status != AccountStatus.SUSPENDED) {
            return false;
        }
        return suspendedUntil == null || suspendedUntil.isAfter(Objects.requireNonNull(now, "now"));
    }

    static AccountStatus effectiveStatus(AccountStatus status, Instant suspendedUntil, Instant now) {
        if (status == AccountStatus.DELETED) {
            return AccountStatus.DELETED;
        }
        return isSuspended(status, suspendedUntil, now) ? AccountStatus.SUSPENDED : AccountStatus.ACTIVE;
    }
}
