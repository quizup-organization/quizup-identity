package io.github.quizup.identity.infrastructure.security;

import io.github.quizup.identity.application.service.SocialUserService;
import io.github.quizup.identity.domain.exception.UserReportProblems;
import io.github.quizup.identity.domain.model.SocialProvider;
import io.github.quizup.identity.domain.model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * Service OAuth2/OIDC pour le login social navigateur (Google, etc.).
 *
 * <p>La résolution/création de l'utilisateur est déléguée à {@link SocialUserService},
 * partagé avec le login Google natif mobile. Un compte suspendu est refusé et converti
 * en échec OAuth2 pour déclencher le handler de redirection existant.</p>
 */
@Service
public class OAuth2UserServiceImpl extends OidcUserService {

    private static final Logger logger = LoggerFactory.getLogger(OAuth2UserServiceImpl.class);

    private final SocialUserService socialUserService;
    private final Roles roles;

    public OAuth2UserServiceImpl(SocialUserService socialUserService, Roles roles) {
        this.socialUserService = socialUserService;
        this.roles = roles;
    }

    @Override
    public OidcUser loadUser(OidcUserRequest userRequest) throws OAuth2AuthenticationException {
        OidcUser oidcUser = super.loadUser(userRequest);

        Map<String, Object> attributes = oidcUser.getAttributes();
        String email = oidcUser.getEmail();
        String provider = userRequest.getClientRegistration().getRegistrationId().toUpperCase();
        logger.debug("OAuth2 user loaded: email={}, provider={}", email, provider);

        if (email == null || email.isBlank()) {
            throw new OAuth2AuthenticationException("Email not provided by OAuth2 provider");
        }

        SocialProvider socialProvider;
        try {
            socialProvider = SocialProvider.valueOf(provider);
        } catch (IllegalArgumentException exception) {
            logger.error("Unknown social provider: {}", provider);
            throw new OAuth2AuthenticationException("Unsupported social provider: " + provider);
        }

        try {
            User user = socialUserService.resolveOrCreate(email, socialProvider);
            return principal(user.userId(), user.email(), oidcUser, attributes);
        } catch (UserReportProblems.AccountSuspendedProblem exception) {
            Object userId = exception.getContext() == null ? null : exception.getContext().get("userId");
            logger.warn("OAuth2 login refused for suspended user: userId={}", userId);
            throw new OAuth2AuthenticationException(new OAuth2Error(
                    "account_suspended",
                    "The account is suspended",
                    null));
        } catch (RuntimeException exception) {
            throw new OAuth2AuthenticationException("Failed to resolve social user: " + exception.getMessage());
        }
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
}
