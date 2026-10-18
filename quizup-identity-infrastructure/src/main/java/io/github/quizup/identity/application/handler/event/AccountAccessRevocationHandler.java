package io.github.quizup.identity.application.handler.event;

import io.github.quizup.identity.application.service.AccountAccessRevocationService;
import io.github.quizup.identity.domain.event.UserEvent;
import org.axonframework.config.ProcessingGroup;
import org.axonframework.eventhandling.EventHandler;
import org.springframework.stereotype.Component;

/**
 * Révoque les accès persistés (sessions {@code AUTH_TX}, autorisations OAuth2) à la suspension.
 *
 * <p>Le service vérifie le statut courant et le {@code suspensionId} : une relecture d'une
 * ancienne suspension ne déconnecte pas un compte depuis réintégré ou re-suspendu.</p>
 */
@Component
@ProcessingGroup("account-access-revocation")
public class AccountAccessRevocationHandler {

    private final AccountAccessRevocationService revocationService;

    public AccountAccessRevocationHandler(AccountAccessRevocationService revocationService) {
        this.revocationService = revocationService;
    }

    @EventHandler
    public void on(UserEvent.UserSuspendedEvent event) {
        revocationService.revokeIfCurrent(event.userId(), event.suspensionId());
    }
}
