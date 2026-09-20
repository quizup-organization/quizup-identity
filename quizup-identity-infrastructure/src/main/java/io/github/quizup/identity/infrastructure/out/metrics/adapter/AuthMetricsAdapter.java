package io.github.quizup.identity.infrastructure.out.metrics.adapter;

import io.github.quizup.identity.domain.port.out.AuthMetricsPort;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

/**
 * Adapter Micrometer du {@link AuthMetricsPort}.
 *
 * <p>Les tags communs {@code application}/{@code environment}/{@code version} sont ajoutés
 * automatiquement par le SDK.
 */
@Component
public class AuthMetricsAdapter implements AuthMetricsPort {

    private final MeterRegistry registry;

    public AuthMetricsAdapter(MeterRegistry registry) {
        this.registry = registry;
    }

    @Override
    public void loginSucceeded() {
        Counter.builder("quizup.auth.logins")
                .tag("result", "success")
                .register(registry)
                .increment();
    }

    @Override
    public void loginFailed() {
        Counter.builder("quizup.auth.logins")
                .tag("result", "failure")
                .register(registry)
                .increment();
    }

    @Override
    public void registered(String method) {
        Counter.builder("quizup.auth.registrations")
                .tag("method", safe(method))
                .register(registry)
                .increment();
    }

    @Override
    public void socialProviderLinked(String provider) {
        Counter.builder("quizup.auth.social.links")
                .tag("provider", safe(provider))
                .register(registry)
                .increment();
    }

    private static String safe(String value) {
        return value == null || value.isBlank() ? "unknown" : value;
    }
}
