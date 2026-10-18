package io.github.quizup.identity.application.handler.event;

import io.github.quizup.identity.domain.event.UserEvent;
import org.axonframework.config.ProcessingGroup;
import org.axonframework.eventhandling.EventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Révoque immédiatement les accès persistés d'un compte suspendu :
 * sessions interactives {@code AUTH_TX} (Spring Session JDBC) et autorisations OAuth2
 * (codes, access tokens et refresh tokens). Les access tokens déjà émis restent
 * cryptographiquement valides jusqu'à leur expiration ; le BFF les refuse via son filtre.
 *
 * <p>Handler idempotent : une relecture de l'événement supprime simplement des lignes déjà
 * absentes.</p>
 */
@Component
@ProcessingGroup("account-access-revocation")
public class AccountAccessRevocationHandler {

    private static final Logger logger = LoggerFactory.getLogger(AccountAccessRevocationHandler.class);

    private final JdbcTemplate jdbcTemplate;

    public AccountAccessRevocationHandler(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @EventHandler
    @Transactional
    public void on(UserEvent.UserSuspendedEvent event) {
        int sessions = jdbcTemplate.update(
                "DELETE FROM spring_session WHERE principal_name = ?", event.userId());
        int authorizations = jdbcTemplate.update(
                "DELETE FROM oauth2_authorization WHERE principal_name = ?", event.userId());
        logger.info("Access revoked for suspended user {}: {} session(s), {} authorization(s)",
                event.userId(), sessions, authorizations);
    }
}
