package io.github.quizup.identity.domain.model;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UserReportRulesTest {

    @Test
    void validateReport_requiresAtLeastOneReason() {
        assertThrows(IllegalArgumentException.class,
                () -> UserReportRules.validateReport(Set.of(), null));
    }

    @Test
    void validateReport_acceptsReasonsAndBoundedComment() {
        assertDoesNotThrow(() -> UserReportRules.validateReport(
                Set.of(UserReportReason.SPAM), "x".repeat(UserReportRules.MAX_COMMENT_LENGTH)));
    }

    @Test
    void validateReport_rejectsTooLongComment() {
        assertThrows(IllegalArgumentException.class, () -> UserReportRules.validateReport(
                Set.of(UserReportReason.SPAM), "x".repeat(UserReportRules.MAX_COMMENT_LENGTH + 1)));
    }

    @Test
    void validateSuspension_rejectsPastEnd() {
        Instant now = Instant.parse("2026-01-01T00:00:00Z");
        assertThrows(IllegalArgumentException.class,
                () -> UserReportRules.validateSuspension(now, now.minusSeconds(1)));
        assertDoesNotThrow(() -> UserReportRules.validateSuspension(now, null));
        assertDoesNotThrow(() -> UserReportRules.validateSuspension(now, now.plusSeconds(1)));
    }

    @Test
    void accountStatus_expiredSuspensionIsEffectivelyActive() {
        Instant now = Instant.parse("2026-01-02T00:00:00Z");
        assertFalse(AccountStatusRules.isSuspended(
                AccountStatus.SUSPENDED, now.minusSeconds(1), now));
        assertTrue(AccountStatusRules.isSuspended(
                AccountStatus.SUSPENDED, now.plusSeconds(1), now));
        assertTrue(AccountStatusRules.isSuspended(AccountStatus.SUSPENDED, null, now));
    }
}
