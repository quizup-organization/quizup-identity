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
 * DataSeeder - Initialise les données de test au démarrage.
 * <p>
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

        logger.info("=== Starting User Data Seeding ===");

        seedUser(QuizUpConstants.ADMIN_USER_ID, QuizUpConstants.ADMIN_USER_EMAIL, "admin123", "Admin");
        seedUser(QuizUpConstants.BOT_USER_ID, QuizUpConstants.BOT_USER_EMAIL, "bot1234", "Bot");

        logger.info("=== User Data Seeding Completed ===");
    }

    private void seedUser(String userId, String email, String password, String label) {
        if (checkUserUseCase.existsById(userId).join()) {
            logger.info("{} user already exists, skipping creation", label);
            return;
        }

        logger.info("Creating {} user: {}", label, email);

        try {
            registerUserUseCase.registerWithPassword(userId, email, password).join();
            logger.info("✓ {} user created: {}", label, userId);
        } catch (CompletionException exception) {
            if (isAggregateAlreadyExists(exception)) {
                logger.info("{} user already exists in the event store, skipping creation", label);
            } else {
                logger.error("Failed to seed {} user", label, exception);
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
