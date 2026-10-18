package io.github.quizup.identity.infrastructure.security;

import io.github.quizup.identity.domain.port.in.PasswordlessAuthUseCase;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Service;

/**
 * Établit la session interactive de la SPA web (Spring Session JDBC, cookie {@code AUTH_TX}).
 *
 * <p>La session ne contient pas les tokens : elle authentifie l'utilisateur pour
 * {@code /oauth2/authorize}, qui poursuit le flux Authorization Code + PKCE standard.</p>
 */
@Service
public class WebSessionService {

    private final SecurityContextRepository securityContextRepository;
    private final Roles roles;

    public WebSessionService(SecurityContextRepository securityContextRepository, Roles roles) {
        this.securityContextRepository = securityContextRepository;
        this.roles = roles;
    }

    public void establishSession(PasswordlessAuthUseCase.AuthenticatedUser user,
                                 HttpServletRequest request,
                                 HttpServletResponse response) {
        // Anti-fixation de session : renouvelle l'identifiant de session si elle existe.
        if (request.getSession(false) != null) {
            request.changeSessionId();
        }

        UserPrincipal principal = new UserPrincipal(
                user.userId(),
                user.email(),
                roles.forUser(user.userId(), user.email())
        );
        Authentication authentication = UsernamePasswordAuthenticationToken.authenticated(
                principal, null, principal.getAuthorities());

        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
        securityContextRepository.saveContext(context, request, response);
    }
}
