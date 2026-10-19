package io.github.quizup.identity.infrastructure.security;

import io.github.quizup.identity.application.service.SocialUserService;
import io.github.quizup.identity.domain.exception.UserReportProblems;
import io.github.quizup.identity.domain.model.SocialProvider;
import io.github.quizup.identity.domain.model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * Service OAuth2 du login Facebook navigateur (flux « classique »).
 *
 * <p>Facebook web n'est pas un provider OIDC : Spring récupère un access token, puis
 * appelle l'endpoint Graph {@code /me?fields=id,name,email,picture}. L'email est la clé
 * de résolution du compte QuizUp ; s'il est absent (compte Facebook sans email ou
 * permission refusée), la connexion est refusée et la SPA propose le repli OTP.</p>
 *
 * <p>Le endpoint Graph est appelé par {@link DefaultOAuth2UserService} avec le token
 * en en-tête Bearer. Laisser le réglage Meta « Require App Secret » désactivé pour ce
 * flux web (ou ajouter {@code appsecret_proof} via un client Graph dédié).</p>
 */
@Service
public class FacebookOAuth2UserService extends DefaultOAuth2UserService {

    private static final Logger logger = LoggerFactory.getLogger(FacebookOAuth2UserService.class);

    /** Code d'erreur OAuth2 utilisé par le failure handler pour rediriger la SPA. */
    public static final String EMAIL_MISSING_ERROR_CODE = "email_missing";

    private final SocialUserService socialUserService;
    private final Roles roles;

    public FacebookOAuth2UserService(SocialUserService socialUserService, Roles roles) {
        this.socialUserService = socialUserService;
        this.roles = roles;
    }

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        return resolve(super.loadUser(userRequest));
    }

    UserPrincipal resolve(OAuth2User oauth2User) {
        String email = oauth2User.getAttribute("email");
        if (!StringUtils.hasText(email)) {
            logger.warn("Facebook login refused: provider did not return an email");
            throw new OAuth2AuthenticationException(new OAuth2Error(
                    EMAIL_MISSING_ERROR_CODE,
                    "Facebook did not provide an email",
                    null));
        }

        try {
            User user = socialUserService.resolveOrCreate(email, SocialProvider.FACEBOOK);
            logger.debug("Facebook user resolved: userId={}", user.userId());
            return new UserPrincipal(
                    user.userId(),
                    user.email(),
                    oauth2User.getAttributes(),
                    roles.forUser(user.userId(), user.email())
            );
        } catch (UserReportProblems.AccountSuspendedProblem exception) {
            Object userId = exception.getContext() == null ? null : exception.getContext().get("userId");
            logger.warn("Facebook login refused for suspended user: userId={}", userId);
            throw new OAuth2AuthenticationException(new OAuth2Error(
                    "account_suspended",
                    "The account is suspended",
                    null));
        } catch (RuntimeException exception) {
            logger.error("Failed to resolve Facebook user", exception);
            throw new OAuth2AuthenticationException("Failed to resolve Facebook user: " + exception.getMessage());
        }
    }
}
