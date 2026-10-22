package io.github.quizup.identity.application.service;

import io.github.quizup.identity.domain.exception.UserProblems;
import io.github.quizup.identity.domain.model.AccountStatus;
import io.github.quizup.identity.domain.model.LoginCodeRules;
import io.github.quizup.identity.domain.model.User;
import io.github.quizup.identity.domain.port.out.EmailSenderPort;
import io.github.quizup.identity.domain.port.out.UserRepositoryPort;
import io.github.quizup.microservice.core.domain.constant.QuizUpConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Locale;
import java.util.Optional;

/**
 * Codes OTP dédiés à la confirmation d'une suppression de compte. Table et TTL séparés des
 * codes de connexion : un code de suppression ne peut jamais ouvrir une session.
 */
@Service
public class AccountDeletionCodeService {

    private static final Logger logger = LoggerFactory.getLogger(AccountDeletionCodeService.class);
    private static final SecureRandom RANDOM = new SecureRandom();

    private final JdbcTemplate jdbcTemplate;
    private final PasswordEncoder passwordEncoder;
    private final EmailSenderPort emailSender;
    private final UserRepositoryPort userRepository;

    public AccountDeletionCodeService(JdbcTemplate jdbcTemplate,
                                      PasswordEncoder passwordEncoder,
                                      EmailSenderPort emailSender,
                                      UserRepositoryPort userRepository) {
        this.jdbcTemplate = jdbcTemplate;
        this.passwordEncoder = passwordEncoder;
        this.emailSender = emailSender;
        this.userRepository = userRepository;
    }

    /** Demande un code ; reste silencieux si le compte n'existe pas ou n'est pas actif. */
    @Transactional
    public void requestCode(String email) {
        String normalized = normalize(email);
        if (normalized == null || QuizUpConstants.SYSTEM_USER_EMAIL.equalsIgnoreCase(normalized)) {
            return;
        }
        Optional<User> user = userRepository.findByEmail(normalized);
        if (user.isEmpty() || user.get().status() != AccountStatus.ACTIVE) {
            return;
        }

        Instant now = Instant.now();
        Integer recent = jdbcTemplate.query(
                "SELECT 1 FROM account_deletion_code "
                        + "WHERE email = ? AND user_id = ? AND created_at > ? LIMIT 1",
                rs -> rs.next() ? 1 : null,
                normalized,
                user.get().userId(),
                Timestamp.from(now.minus(LoginCodeRules.RESEND_COOLDOWN)));
        if (recent != null) {
            logger.info("Account deletion code requested too soon - ignored");
            return;
        }

        String code = generateCode();
        jdbcTemplate.update("""
                INSERT INTO account_deletion_code
                    (email, user_id, code_hash, attempts, expires_at, created_at, consumed_at)
                VALUES (?, ?, ?, 0, ?, ?, NULL)
                ON CONFLICT (email) DO UPDATE SET
                    user_id = EXCLUDED.user_id,
                    code_hash = EXCLUDED.code_hash,
                    attempts = 0,
                    expires_at = EXCLUDED.expires_at,
                    created_at = EXCLUDED.created_at,
                    consumed_at = NULL
                """,
                normalized,
                user.get().userId(),
                passwordEncoder.encode(code),
                Timestamp.from(now.plus(LoginCodeRules.TTL)),
                Timestamp.from(now));

        try {
            emailSender.sendAccountDeletionCode(normalized, code, LoginCodeRules.TTL);
        } catch (RuntimeException exception) {
            jdbcTemplate.update(
                    "DELETE FROM account_deletion_code WHERE email = ? AND user_id = ?",
                    normalized, user.get().userId());
            logger.error("Failed to send account deletion code", exception);
        }
    }

    /** Vérifie et consomme le code. Toute erreur est volontairement générique. */
    @Transactional
    public void verifyCode(String email, String code) {
        String normalized = normalize(email);
        Instant now = Instant.now();
        CodeRow row = jdbcTemplate.query("""
                        SELECT code_hash, attempts, expires_at, consumed_at
                        FROM account_deletion_code WHERE email = ?
                        """,
                rs -> rs.next() ? new CodeRow(
                        rs.getString("code_hash"),
                        rs.getInt("attempts"),
                        rs.getTimestamp("expires_at").toInstant(),
                        rs.getTimestamp("consumed_at") == null ? null : rs.getTimestamp("consumed_at").toInstant())
                        : null,
                normalized);

        if (row == null || row.consumedAt() != null || !row.expiresAt().isAfter(now)
                || row.attempts() >= LoginCodeRules.MAX_ATTEMPTS
                || code == null || !passwordEncoder.matches(code, row.codeHash())) {
            if (row != null && row.attempts() < LoginCodeRules.MAX_ATTEMPTS) {
                jdbcTemplate.update(
                        "UPDATE account_deletion_code SET attempts = attempts + 1 WHERE email = ?",
                        normalized);
            }
            throw new UserProblems.InvalidAccountDeletionCodeProblem(normalized);
        }

        jdbcTemplate.update(
                "UPDATE account_deletion_code SET consumed_at = ? WHERE email = ?",
                Timestamp.from(now), normalized);
    }

    private static String generateCode() {
        return String.format(Locale.ROOT, "%06d", RANDOM.nextInt(1_000_000));
    }

    private static String normalize(String email) {
        if (email == null) return null;
        String normalized = email.trim().toLowerCase(Locale.ROOT);
        return normalized.isEmpty() ? null : normalized;
    }

    private record CodeRow(String codeHash, int attempts, Instant expiresAt, Instant consumedAt) {
    }
}
