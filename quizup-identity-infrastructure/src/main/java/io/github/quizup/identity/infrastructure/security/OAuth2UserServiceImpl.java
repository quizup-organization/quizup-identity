package io.github.quizup.identity.infrastructure.security;

import io.github.quizup.identity.domain.port.in.LinkSocialProviderUseCase;
import io.github.quizup.identity.domain.port.in.RegisterUserUseCase;
import io.github.quizup.identity.domain.model.SocialProvider;
import io.github.quizup.identity.domain.model.User;
import io.github.quizup.identity.domain.port.out.UserRepositoryPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Service OAuth2/OIDC pour gérer le login social (Google, etc.).
 * Crée automatiquement un utilisateur s'il n'existe pas, sinon lie le provider
 * au compte local existant (même email).
 */
@Service
public class OAuth2UserServiceImpl extends OidcUserService {

    private static final Logger logger = LoggerFactory.getLogger(OAuth2UserServiceImpl.class);

    private final UserRepositoryPort userRepositoryPort;
    private final RegisterUserUseCase registerUserUseCase;
    private final LinkSocialProviderUseCase linkSocialProviderUseCase;
    private final Roles roles;

    public OAuth2UserServiceImpl(UserRepositoryPort userRepositoryPort,
                                 RegisterUserUseCase registerUserUseCase,
                                 LinkSocialProviderUseCase linkSocialProviderUseCase,
                                 Roles roles) {
        this.userRepositoryPort = userRepositoryPort;
        this.registerUserUseCase = registerUserUseCase;
        this.linkSocialProviderUseCase = linkSocialProviderUseCase;
        this.roles = roles;
    }

    @Override
    public OidcUser loadUser(OidcUserRequest userRequest) throws OAuth2AuthenticationException {
        OidcUser oidcUser = super.loadUser(userRequest);

        Map<String, Object> attributes = oidcUser.getAttributes();
        String email = oidcUser.getEmail();

        String provider = userRequest.getClientRegistration().getRegistrationId().toUpperCase();
        logger.debug("OAuth2 user loaded: email={}, provider={}", email, provider);

        if (email == null) {
            throw new OAuth2AuthenticationException("Email not provided by OAuth2 provider");
        }

        SocialProvider socialProvider;
        try {
            socialProvider = SocialProvider.valueOf(provider);
        } catch (IllegalArgumentException exception) {
            logger.error("Unknown social provider: {}", provider);
            throw new OAuth2AuthenticationException("Unsupported social provider: " + provider);
        }

        Optional<User> optionalUser = userRepositoryPort.findByEmail(email);

        if (optionalUser.isPresent()) {
            User user = optionalUser.get();
            linkIfNeeded(user, socialProvider);
            return principal(user.userId(), user.email(), oidcUser, attributes);
        }

        logger.info("Creating new user from OAuth2: email={}, provider={}", email, provider);
        String userId = UUID.randomUUID().toString();

        try {
            registerUserUseCase.registerWithSocial(userId, email, socialProvider).join();
        } catch (Exception exception) {
            // Course possible avec un enregistrement concurrent : on retente la lecture.
            User existing = userRepositoryPort.findByEmail(email)
                    .orElseThrow(() -> new OAuth2AuthenticationException("Failed to create user: " + exception.getMessage()));
            linkIfNeeded(existing, socialProvider);
            return principal(existing.userId(), existing.email(), oidcUser, attributes);
        }

        return principal(userId, email, oidcUser, attributes);
    }

    private OidcUserPrincipal principal(String userId, String email, OidcUser oidcUser,
                                        Map<String, Object> attributes) {
        return new OidcUserPrincipal(
                userId,
                email,
                oidcUser.getIdToken(),
                oidcUser.getUserInfo(),
                attributes,
                roles.forUser(userId, email)
        );
    }

    private void linkIfNeeded(User user, SocialProvider socialProvider) {
        if (user.linkedSocialAccounts() != null && user.linkedSocialAccounts().contains(socialProvider)) {
            return;
        }
        try {
            linkSocialProviderUseCase.linkSocialProvider(user.userId(), socialProvider).join();
        } catch (Exception exception) {
            logger.warn("Failed to link provider {} to user {}: {}", socialProvider, user.userId(), exception.getMessage());
        }
    }
}
