package io.github.quizup.identity.infrastructure.in.web;

import io.github.quizup.identity.domain.exception.UserProblems;
import io.github.quizup.identity.domain.port.in.PasswordlessAuthUseCase;
import io.github.quizup.identity.infrastructure.security.Roles;
import io.github.quizup.identity.infrastructure.security.UserPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

/**
 * API JSON d'authentification pilotée par la SPA/mobile.
 * <p>
 * Authentification <b>passwordless</b> par code à usage unique (OTP email). L'endpoint
 * de vérification établit une session interactive temporaire (Spring Session JDBC, cookie
 * {@code AUTH_TX}) puis la SPA poursuit le pipeline standard Authorization Code + PKCE.
 * Aucun JWT n'est émis ici.
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    private final PasswordlessAuthUseCase passwordlessAuthUseCase;
    private final SecurityContextRepository securityContextRepository;
    private final Roles roles;
    private final OAuth2AuthorizationService authorizationService;

    public AuthController(PasswordlessAuthUseCase passwordlessAuthUseCase,
                          SecurityContextRepository securityContextRepository,
                          Roles roles,
                          OAuth2AuthorizationService authorizationService) {
        this.passwordlessAuthUseCase = passwordlessAuthUseCase;
        this.securityContextRepository = securityContextRepository;
        this.roles = roles;
        this.authorizationService = authorizationService;
    }

    /**
     * Demande l'envoi d'un code de connexion (ressource {@code login-codes}).
     * Répond toujours 202 (anti-énumération).
     */
    @PostMapping("/login-codes")
    public ResponseEntity<Void> requestCode(@Valid @RequestBody RequestCodeRequest request) {
        passwordlessAuthUseCase.requestCode(request.email());
        return ResponseEntity.accepted().build();
    }

    /**
     * Vérifie le code et établit la session interactive (ressource {@code sessions}).
     * Crée le compte au premier login. REST : création → 201 + Location de la session courante.
     */
    @PostMapping("/sessions")
    public ResponseEntity<AuthResponse> verifyCode(@Valid @RequestBody VerifyCodeRequest request,
                                                   HttpServletRequest httpRequest,
                                                   HttpServletResponse httpResponse) {
        PasswordlessAuthUseCase.AuthenticatedUser user =
                passwordlessAuthUseCase.verifyCode(request.email(), request.code());

        establishSession(user, httpRequest, httpResponse);
        logger.info("Session established via login code: userId={}", user.userId());
        return ResponseEntity
                .created(URI.create("/api/auth/sessions/current"))
                .body(new AuthResponse(user.userId(), user.email()));
    }

    /**
     * Ferme la session courante (ressource {@code sessions/current}). REST : retrait → 204.
     */
    @DeleteMapping("/sessions/current")
    public ResponseEntity<Void> logout(@RequestBody(required = false) LogoutRequest request,
                                       HttpServletRequest httpRequest) {
        if (request != null && StringUtils.hasText(request.refreshToken())) {
            revokeAuthorization(request.refreshToken());
        }
        HttpSession session = httpRequest.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        SecurityContextHolder.clearContext();
        return ResponseEntity.noContent().build();
    }

    private void revokeAuthorization(String refreshToken) {
        OAuth2Authorization authorization =
                authorizationService.findByToken(refreshToken, OAuth2TokenType.REFRESH_TOKEN);
        if (authorization != null) {
            authorizationService.remove(authorization);
            logger.info("Revoked OAuth2 authorization on logout: principal={}", authorization.getPrincipalName());
        }
    }

    @ExceptionHandler(UserProblems.InvalidLoginCodeProblem.class)
    public ResponseEntity<ProblemDetail> handleInvalidLoginCode(UserProblems.InvalidLoginCodeProblem exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, "Code de connexion invalide ou expiré");
        problem.setTitle("Unauthorized");
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(problem);
    }

    private void establishSession(PasswordlessAuthUseCase.AuthenticatedUser user,
                                  HttpServletRequest httpRequest,
                                  HttpServletResponse httpResponse) {
        // Anti-fixation de session : renouvelle l'identifiant de session si elle existe.
        if (httpRequest.getSession(false) != null) {
            httpRequest.changeSessionId();
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
        securityContextRepository.saveContext(context, httpRequest, httpResponse);
    }
}
