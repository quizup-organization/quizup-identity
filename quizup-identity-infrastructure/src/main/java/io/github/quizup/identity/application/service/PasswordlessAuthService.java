package io.github.quizup.identity.application.service;

import io.github.quizup.identity.domain.exception.UserProblems;
import io.github.quizup.identity.domain.model.LoginCode;
import io.github.quizup.identity.domain.model.LoginCodeRules;
import io.github.quizup.identity.domain.model.User;
import io.github.quizup.identity.domain.port.in.FindUserUseCase;
import io.github.quizup.identity.domain.port.in.PasswordlessAuthUseCase;
import io.github.quizup.identity.domain.port.in.RegisterUserUseCase;
import io.github.quizup.identity.domain.port.out.EmailSenderPort;
import io.github.quizup.identity.domain.port.out.LoginCodeRepositoryPort;
import io.github.quizup.identity.infrastructure.properties.AppProperties;
import io.github.quizup.microservice.core.domain.constant.QuizUpConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletionException;

/**
 * Authentification passwordless par code à usage unique (OTP email).
 *
 * <p>Anti-énumération : {@link #requestCode(String)} réussit toujours et n'envoie un email
 * que pour les comptes éligibles. Le premier login crée le compte.</p>
 */
@Service
public class PasswordlessAuthService implements PasswordlessAuthUseCase {

    private static final Logger logger = LoggerFactory.getLogger(PasswordlessAuthService.class);

    private final LoginCodeRepositoryPort loginCodeRepository;
    private final EmailSenderPort emailSender;
    private final FindUserUseCase findUserUseCase;
    private final RegisterUserUseCase registerUserUseCase;
    private final PasswordEncoder passwordEncoder;
    private final String devFixedCode;
    private final SecureRandom secureRandom = new SecureRandom();

    public PasswordlessAuthService(LoginCodeRepositoryPort loginCodeRepository,
                                   EmailSenderPort emailSender,
                                   FindUserUseCase findUserUseCase,
                                   RegisterUserUseCase registerUserUseCase,
                                   PasswordEncoder passwordEncoder,
                                   AppProperties properties) {
        this.loginCodeRepository = loginCodeRepository;
        this.emailSender = emailSender;
        this.findUserUseCase = findUserUseCase;
        this.registerUserUseCase = registerUserUseCase;
        this.passwordEncoder = passwordEncoder;
        this.devFixedCode = properties.auth().devFixedCode();
    }

    @Override
    public void requestCode(String email) {
        String normalized = normalize(email);

        if (QuizUpConstants.SYSTEM_USER_EMAIL.equalsIgnoreCase(normalized)) {
            logger.info("Login code requested for the system account - ignored");
            return;
        }

        Instant now = Instant.now();
        Optional<LoginCode> existing = loginCodeRepository.findByEmail(normalized);

        if (existing.filter(code -> code.isActive(now)
                && Duration.between(code.createdAt(), now).compareTo(LoginCodeRules.RESEND_COOLDOWN) < 0)
                .isPresent()) {
            logger.info("Login code requested too soon - ignored");
            return;
        }

        String code = generateCode();
        loginCodeRepository.save(new LoginCode(
                normalized,
                passwordEncoder.encode(code),
                0,
                now.plus(LoginCodeRules.TTL),
                now,
                null
        ));

        try {
            emailSender.sendLoginCode(normalized, code, LoginCodeRules.TTL);
        } catch (RuntimeException exception) {
            // Anti-énumération : l'échec d'envoi ne doit pas être visible. On retire le code
            // pour ne pas bloquer une nouvelle tentative par le cooldown.
            loginCodeRepository.deleteByEmail(normalized);
            logger.error("Failed to send login code", exception);
        }
    }

    @Override
    public AuthenticatedUser verifyCode(String email, String code) {
        String normalized = normalize(email);
        Instant now = Instant.now();

        LoginCode loginCode = loginCodeRepository.findByEmail(normalized)
                .orElseThrow(() -> new UserProblems.InvalidLoginCodeProblem(normalized));

        if (!loginCode.isActive(now) || !loginCode.hasAttemptsLeft()) {
            throw new UserProblems.InvalidLoginCodeProblem(normalized);
        }

        if (code == null || !passwordEncoder.matches(code, loginCode.codeHash())) {
            loginCodeRepository.save(loginCode.withFailedAttempt());
            throw new UserProblems.InvalidLoginCodeProblem(normalized);
        }

        loginCodeRepository.save(loginCode.consume(now));

        User user = findUserUseCase.findByEmail(normalized).join()
                .orElseGet(() -> createUser(normalized));

        return new AuthenticatedUser(user.userId(), user.email());
    }

    private User createUser(String email) {
        String userId = UUID.randomUUID().toString();

        try {
            registerUserUseCase.registerUser(userId, email).join();
        } catch (CompletionException exception) {
            // Course possible avec un enregistrement concurrent (ex. social) : on relit.
            return findUserUseCase.findByEmail(email).join()
                    .orElseThrow(() -> exception);
        }

        return new User(userId, email, java.util.Set.of(), Instant.now());
    }

    private String generateCode() {
        if (devFixedCode != null && !devFixedCode.isBlank()) {
            return devFixedCode;
        }
        int bound = (int) Math.pow(10, LoginCodeRules.CODE_LENGTH);
        int value = secureRandom.nextInt(bound);
        return String.format("%0" + LoginCodeRules.CODE_LENGTH + "d", value);
    }

    private String normalize(String email) {
        if (email == null || email.isBlank()) {
            throw new UserProblems.InvalidEmailFormatProblem("UNKNOWN", email);
        }
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
