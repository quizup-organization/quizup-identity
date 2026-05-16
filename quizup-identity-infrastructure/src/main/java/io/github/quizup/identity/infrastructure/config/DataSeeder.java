package io.github.quizup.identity.infrastructure.config;

import io.github.quizup.common.domain.constant.QuizUpConstants;
import io.github.quizup.identity.domain.command.UserCommand;
import io.github.quizup.identity.domain.port.in.CheckUserUseCase;
import io.github.quizup.identity.domain.port.in.RegisterUserUseCase;
import io.github.quizup.identity.domain.query.UserQuery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * DataSeeder - Initialise les données de test au démarrage
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

        try {
            seedAdminUser();
            seedTestUser();
            logger.info("=== User Data Seeding Completed Successfully ===");
        } catch (Exception e) {
            logger.error("Error during data seeding", e);
        }
    }

    /**
     * Crée l'utilisateur admin QuizMeUp
     */
    private void seedAdminUser() {
        if (checkUserUseCase.existsById(QuizUpConstants.ADMIN_USER_ID).join()) {
            logger.info("Admin user already exists, skipping creation");
            return;
        }

        logger.info("Creating admin user: {}", QuizUpConstants.ADMIN_USER_EMAIL);

        registerUserUseCase.registerWithPassword(
                QuizUpConstants.ADMIN_USER_ID,
                QuizUpConstants.ADMIN_USER_EMAIL,
                "admin123"
        );

        logger.info("✓ Admin user created: {}", QuizUpConstants.ADMIN_USER_ID);
    }

    /**
     * Crée un utilisateur de test pour les parties
     */
    private void seedTestUser() {
        if (checkUserUseCase.existsById(QuizUpConstants.TEST_USER_ID).join()) {
            logger.info("Test user already exists, skipping creation");
            return;
        }

        logger.info("Creating test user: {}", QuizUpConstants.TEST_USER_EMAIL);

        registerUserUseCase.registerWithPassword(
                QuizUpConstants.TEST_USER_ID,
                QuizUpConstants.TEST_USER_EMAIL,
                "test1234"
        );

        logger.info("✓ Test user created: {}", QuizUpConstants.TEST_USER_ID);
    }
}
