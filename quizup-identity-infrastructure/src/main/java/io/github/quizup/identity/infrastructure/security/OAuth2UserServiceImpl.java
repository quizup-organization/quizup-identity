package io.github.quizup.identity.infrastructure.security;

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
 * Service OAuth2/OIDC pour gérer le login social (Google, Facebook, etc.)
 * Crée automatiquement un utilisateur s'il n'existe pas.
 * Gère à la fois OAuth2 standard et OIDC.
 */
@Service
public class OAuth2UserServiceImpl extends OidcUserService {

    private static final Logger logger = LoggerFactory.getLogger(OAuth2UserServiceImpl.class);

    private final UserRepositoryPort userRepositoryPort;
    private final RegisterUserUseCase registerUserUseCase;

    public OAuth2UserServiceImpl(UserRepositoryPort userRepositoryPort, RegisterUserUseCase registerUserUseCase) {
        this.userRepositoryPort = userRepositoryPort;
        this.registerUserUseCase = registerUserUseCase;
    }

    @Override
    public OidcUser loadUser(OidcUserRequest userRequest) throws OAuth2AuthenticationException {
        logger.info("===== OAuth2UserServiceImpl.loadUser() CALLED (OIDC) =====");
        OidcUser oidcUser = super.loadUser(userRequest);

        Map<String, Object> attributes = oidcUser.getAttributes();
        String email = oidcUser.getEmail();

        // Récupérer le provider (GOOGLE, FACEBOOK, etc.)
        String provider = userRequest.getClientRegistration().getRegistrationId().toUpperCase();

        logger.debug("OAuth2 user loaded: email={}, provider={}", email, provider);

        if (email == null) {
            throw new OAuth2AuthenticationException("Email not provided by OAuth2 provider");
        }

        // Convertir le provider string en SocialProvider enum
        SocialProvider socialProvider;

        try {
            socialProvider = SocialProvider.valueOf(provider);
        } catch (IllegalArgumentException exception) {
            logger.error("Unknown social provider: {}", provider);
            throw new OAuth2AuthenticationException("Unsupported social provider: " + provider);
        }

        // Chercher l'utilisateur existant
        Optional<User> optionalUser = userRepositoryPort.findByEmail(email);

        if (optionalUser.isPresent()) {
            // Utilisateur existe déjà
            logger.debug("Existing user found for email: {}", email);
            User user = optionalUser.get();
            return new OidcUserPrincipal(
                    user.userId(),
                    user.email(),
                    oidcUser.getIdToken(),
                    oidcUser.getUserInfo(),
                    attributes
            );
        }

        // Créer un nouvel utilisateur avec le provider
        logger.info("Creating new user from OAuth2: email={}, provider={}", email, provider);
        String userId = UUID.randomUUID().toString();

        try {
            registerUserUseCase.registerWithSocial(
                    userId,
                    email,
                    socialProvider
            );
            logger.info("New OAuth2 user created: userId={}, provider={}", userId, provider);
        } catch (Exception exception) {
            logger.error("Failed to create OAuth2 user", exception);
            throw new OAuth2AuthenticationException("Failed to create user: " + exception.getMessage());
        }

        return new OidcUserPrincipal(userId, email, oidcUser.getIdToken(), oidcUser.getUserInfo(), attributes);
    }
}
