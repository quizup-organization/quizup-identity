package io.github.quizup.identity.infrastructure.config;

import io.github.quizup.microservice.core.domain.constant.QuizUpConstants;
import io.github.quizup.identity.domain.port.in.CheckUserUseCase;
import io.github.quizup.identity.domain.port.in.RegisterUserUseCase;
import org.axonframework.modelling.command.AggregateStreamCreationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletionException;

/**
 * DataSeeder - Initialise le compte système au démarrage.
 * <p>
 * Le compte système (admin + bot) ne possède aucun credential : il ne peut pas se connecter.
 * Idempotent y compris vis-à-vis de l'event store : si l'agrégat existe déjà
 * (mais que la projection read-model n'est pas encore visible), la tentative de
 * création échoue sur {@link AggregateStreamCreationException} et est ignorée.
 */
@Component
public class DataSeeder implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(DataSeeder.class);
    private final RegisterUserUseCase registerUserUseCase;
    private final CheckUserUseCase checkUserUseCase;

    @Value("${app.seed-data.enabled:false}")
    private boolean seedDataEnabled;

    public DataSeeder(RegisterUserUseCase registerUserUseCase,
                      CheckUserUseCase checkUserUseCase) {
        this.registerUserUseCase = registerUserUseCase;
        this.checkUserUseCase = checkUserUseCase;
    }

    @Override
    public void run(String... args) {
        if (!seedDataEnabled) {
            logger.info("Data seeding is disabled (app.seed-data.enabled=false)");
            return;
        }

        logger.info("=== Starting System User Seeding ===");

        seedSystemUser();

        logger.info("=== System User Seeding Completed ===");
    }

    private void seedSystemUser() {
        String userId = QuizUpConstants.SYSTEM_USER_ID;

        if (checkUserUseCase.existsById(userId).join()) {
            logger.info("System user already exists, skipping creation");
            return;
        }

        logger.info("Creating system user: {}", QuizUpConstants.SYSTEM_USER_EMAIL);

        try {
            registerUserUseCase.registerUser(userId, QuizUpConstants.SYSTEM_USER_EMAIL).join();
            logger.info("✓ System user created: {}", userId);
        } catch (CompletionException exception) {
            if (isAggregateAlreadyExists(exception)) {
                logger.info("System user already exists in the event store, skipping creation");
            } else {
                logger.error("Failed to seed system user", exception);
            }
        }
    }

    private boolean isAggregateAlreadyExists(Throwable throwable) {
        Throwable cause = throwable;
        while (cause != null) {
            if (cause instanceof AggregateStreamCreationException) {
                return true;
            }
            cause = cause.getCause();
        }
        return false;
    }
}
