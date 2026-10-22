package io.github.quizup.identity.application.service;

import io.github.quizup.identity.domain.port.out.EmailClaimPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Nettoyage local d'un compte supprimé : email, codes OTP, sessions et autorisations.
 * Les données des autres services sont nettoyées par leurs handlers du même événement.
 */
@Service
public class AccountDeletionCleanupService {

    private final EmailClaimPort emailClaimPort;
    private final AccountAccessRevocationService accessRevocationService;
    private final JdbcTemplate jdbcTemplate;

    public AccountDeletionCleanupService(EmailClaimPort emailClaimPort,
                                         AccountAccessRevocationService accessRevocationService,
                                         JdbcTemplate jdbcTemplate) {
        this.emailClaimPort = emailClaimPort;
        this.accessRevocationService = accessRevocationService;
        this.jdbcTemplate = jdbcTemplate;
    }

    /** Threads de signalements impliquant le compte, à effacer de l'event store. */
    @Transactional(readOnly = true)
    public java.util.List<String> reportThreadIds(String userId) {
        return jdbcTemplate.queryForList("""
                SELECT DISTINCT thread_id FROM user_report
                WHERE target_user_id = ? OR reporter_id = ?
                """, String.class, userId, userId);
    }

    @Transactional
    public void cleanup(String userId, String oldEmail, String deletionId, java.time.Instant deletedAt) {
        java.time.Instant cutoff = deletedAt == null ? java.time.Instant.now() : deletedAt;
        if (oldEmail != null && !oldEmail.isBlank()) {
            String normalizedEmail = oldEmail.trim().toLowerCase(java.util.Locale.ROOT);
            emailClaimPort.release(normalizedEmail, userId);
            jdbcTemplate.update(
                    "DELETE FROM user_login_code WHERE email = ? AND created_at < ?",
                    normalizedEmail, java.sql.Timestamp.from(cutoff));
            jdbcTemplate.update(
                    "DELETE FROM account_deletion_code WHERE email = ? AND user_id = ?",
                    normalizedEmail, userId);
        }
        jdbcTemplate.update(
                "DELETE FROM user_report WHERE target_user_id = ? OR reporter_id = ?",
                userId, userId);
        jdbcTemplate.update(
                "DELETE FROM user_social_providers_entry WHERE user_id = ?", userId);
        accessRevocationService.revokeAll(userId);
        jdbcTemplate.update("""
                INSERT INTO account_deletion_audit
                    (deletion_id, user_id, requested_at, status)
                VALUES (?, ?, now(), 'REQUESTED')
                ON CONFLICT (deletion_id) DO NOTHING
                """, deletionId, userId);
    }

    /** Marque l'effacement identity terminé (les consommateurs Kafka restent asynchrones). */
    @Transactional
    public void markCompleted(String deletionId) {
        jdbcTemplate.update("""
                UPDATE account_deletion_audit
                SET status = 'COMPLETED', completed_at = now()
                WHERE deletion_id = ?
                """, deletionId);
    }
}
