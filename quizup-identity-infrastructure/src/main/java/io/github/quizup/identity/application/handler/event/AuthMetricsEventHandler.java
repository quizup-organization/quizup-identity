package io.github.quizup.identity.application.handler.event;

import io.github.quizup.identity.domain.event.UserEvent;
import io.github.quizup.identity.domain.port.out.AuthMetricsPort;
import org.axonframework.eventhandling.DisallowReplay;
import org.axonframework.eventhandling.EventHandler;
import org.springframework.stereotype.Component;

/**
 * Alimente les KPI métier d'inscription à partir des événements d'identité.
 *
 * <p>Découplé de la projection read-model ; {@link DisallowReplay} évite de réincrémenter les
 * compteurs lors d'un replay de projection.
 */
@Component
public class AuthMetricsEventHandler {

    private final AuthMetricsPort metrics;

    public AuthMetricsEventHandler(AuthMetricsPort metrics) {
        this.metrics = metrics;
    }

    @EventHandler
    @DisallowReplay
    public void on(UserEvent.UserRegisteredEvent event) {
        metrics.registered(event.provider() == null ? "PASSWORD" : "SOCIAL");
    }

    @EventHandler
    @DisallowReplay
    public void on(UserEvent.SocialProviderLinkedEvent event) {
        metrics.socialProviderLinked(event.provider() == null ? "unknown" : event.provider().name());
    }
}
