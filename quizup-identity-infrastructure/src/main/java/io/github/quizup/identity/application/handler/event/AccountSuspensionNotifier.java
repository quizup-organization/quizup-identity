package io.github.quizup.identity.application.handler.event;

import io.github.quizup.identity.domain.event.UserEvent;
import io.github.quizup.identity.domain.port.out.EmailSenderPort;
import org.axonframework.config.ProcessingGroup;
import org.axonframework.eventhandling.EventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Envoie un email immédiat au joueur suspendu. L'échec d'envoi ne doit jamais annuler
 * la suspension : il est loggé et l'événement reste la source de vérité.
 */
@Component
@ProcessingGroup("account-email")
public class AccountSuspensionNotifier {

    private static final Logger logger = LoggerFactory.getLogger(AccountSuspensionNotifier.class);

    private final EmailSenderPort emailSender;

    public AccountSuspensionNotifier(EmailSenderPort emailSender) {
        this.emailSender = emailSender;
    }

    @EventHandler
    public void on(UserEvent.UserSuspendedEvent event) {
        try {
            emailSender.sendAccountSuspended(event.email(), event.reason(), event.suspendedUntil());
        } catch (RuntimeException error) {
            logger.error("Unable to send suspension email for user {}: {}",
                    event.userId(), error.getMessage());
        }
    }
}
