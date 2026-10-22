package io.github.quizup.identity.application.service;

import io.github.quizup.identity.domain.model.AccountStatus;
import io.github.quizup.identity.domain.model.User;
import io.github.quizup.identity.domain.port.out.UserRepositoryPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Révoque les accès persistés d'un compte suspendu : sessions interactives {@code AUTH_TX}
 * (Spring Session JDBC) et autorisations OAuth2 (codes, access tokens, refresh tokens).
 *
 * <p>Appelé depuis la projection identity, après mise à jour du statut : au rejeu, une
 * ancienne suspension ne déconnecte pas un compte depuis réintégré (garde sur le
 * {@code suspensionId} courant).</p>
 */
@Service
public class AccountAccessRevocationService {

    private static final Logger logger = LoggerFactory.getLogger(AccountAccessRevocationService.class);

    private final JdbcTemplate jdbcTemplate;
    private final UserRepositoryPort userRepository;

    public AccountAccessRevocationService(JdbcTemplate jdbcTemplate,
                                          UserRepositoryPort userRepository) {
        this.jdbcTemplate = jdbcTemplate;
        this.userRepository = userRepository;
    }

    @Transactional
    public void revokeIfCurrent(String userId, String suspensionId) {
        User user = userRepository.findById(userId).orElse(null);
        if (user == null || user.status() != AccountStatus.SUSPENDED) {
            return;
        }
        if (user.suspension() != null
                && !suspensionId.equals(user.suspension().suspensionId())) {
            logger.debug("Access revocation skipped (stale suspension): userId={}", userId);
            return;
        }

        revokeAll(userId);
    }

    /** Révocation inconditionnelle (suppression de compte notamment). */
    @Transactional
    public void revokeAll(String userId) {
        int sessions = jdbcTemplate.update(
                "DELETE FROM spring_session WHERE principal_name = ?", userId);
        int authorizations = jdbcTemplate.update(
                "DELETE FROM oauth2_authorization WHERE principal_name = ?", userId);
        int consents = jdbcTemplate.update(
                "DELETE FROM oauth2_authorization_consent WHERE principal_name = ?", userId);
        logger.info("Access revoked for user {}: {} session(s), {} authorization(s), {} consent(s)",
                userId, sessions, authorizations, consents);
    }
}
