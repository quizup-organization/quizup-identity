package io.github.quizup.identity.application.service;

import io.github.quizup.identity.domain.exception.UserReportProblems;
import io.github.quizup.identity.domain.model.SocialProvider;
import io.github.quizup.identity.domain.model.User;
import io.github.quizup.identity.domain.port.in.LinkSocialProviderUseCase;
import io.github.quizup.identity.domain.port.in.RecordUserLoginUseCase;
import io.github.quizup.identity.domain.port.in.RegisterUserUseCase;
import io.github.quizup.identity.domain.port.out.UserRepositoryPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Résolution d'un utilisateur à partir d'une identité sociale (Google, etc.).
 *
 * <p>Logique partagée par le login Google navigateur ({@code OAuth2UserServiceImpl}) et
 * le login Google natif mobile ({@code MobileAuthController}) : création au premier
 * login, liaison du provider à un compte local existant (même email), contrôle de
 * suspension et enregistrement de la connexion.</p>
 */
@Service
public class SocialUserService {

    private static final Logger logger = LoggerFactory.getLogger(SocialUserService.class);

    private final UserRepositoryPort userRepositoryPort;
    private final RegisterUserUseCase registerUserUseCase;
    private final LinkSocialProviderUseCase linkSocialProviderUseCase;
    private final RecordUserLoginUseCase recordUserLoginUseCase;

    public SocialUserService(UserRepositoryPort userRepositoryPort,
                             RegisterUserUseCase registerUserUseCase,
                             LinkSocialProviderUseCase linkSocialProviderUseCase,
                             RecordUserLoginUseCase recordUserLoginUseCase) {
        this.userRepositoryPort = userRepositoryPort;
        this.registerUserUseCase = registerUserUseCase;
        this.linkSocialProviderUseCase = linkSocialProviderUseCase;
        this.recordUserLoginUseCase = recordUserLoginUseCase;
    }

    /**
     * Retourne l'utilisateur correspondant à l'email social, en le créant si nécessaire.
     *
     * @throws UserReportProblems.AccountSuspendedProblem si le compte est suspendu
     */
    public User resolveOrCreate(String rawEmail, SocialProvider provider) {
        String email = normalize(rawEmail);
        Optional<User> optionalUser = userRepositoryPort.findByEmail(email);

        if (optionalUser.isPresent()) {
            User user = optionalUser.get();
            ensureActive(user);
            linkIfNeeded(user, provider);
            recordUserLogin(user.userId());
            return user;
        }

        logger.info("Creating new user from social provider: email={}, provider={}", email, provider);
        String userId = UUID.randomUUID().toString();

        try {
            registerUserUseCase.registerWithSocial(userId, email, provider).join();
            recordUserLogin(userId);
            return new User(userId, email, Set.of(provider), Instant.now());
        } catch (RuntimeException exception) {
            // Course possible avec un enregistrement concurrent : on relit.
            User existing = userRepositoryPort.findByEmail(email)
                    .orElseThrow(() -> exception);
            ensureActive(existing);
            linkIfNeeded(existing, provider);
            recordUserLogin(existing.userId());
            return existing;
        }
    }

    private void ensureActive(User user) {
        if (user.suspended(Instant.now())) {
            logger.warn("Social login refused for suspended user: userId={}", user.userId());
            throw new UserReportProblems.AccountSuspendedProblem(
                    user.userId(),
                    user.suspension() == null ? null : user.suspension().suspendedUntil());
        }
    }

    private void linkIfNeeded(User user, SocialProvider provider) {
        if (user.linkedSocialAccounts() != null && user.linkedSocialAccounts().contains(provider)) {
            return;
        }
        try {
            linkSocialProviderUseCase.linkSocialProvider(user.userId(), provider).join();
        } catch (Exception exception) {
            logger.warn("Failed to link provider {} to user {}: {}", provider, user.userId(), exception.getMessage());
        }
    }

    private void recordUserLogin(String userId) {
        try {
            recordUserLoginUseCase.recordLogin(userId).join();
        } catch (Exception exception) {
            logger.warn("Failed to record login for user {}: {}", userId, exception.getMessage());
        }
    }

    private String normalize(String email) {
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("Email is required for social login");
        }
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
