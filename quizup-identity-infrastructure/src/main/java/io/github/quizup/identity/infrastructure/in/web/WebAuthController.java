package io.github.quizup.identity.infrastructure.in.web;

import io.github.quizup.identity.application.service.AccountDeletionCodeService;
import io.github.quizup.identity.domain.port.in.PasswordlessAuthUseCase;
import io.github.quizup.identity.infrastructure.security.AuthorizationRevocationService;
import io.github.quizup.identity.infrastructure.security.WebSessionService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

/**
 * Authentification de la SPA web.
 *
 * <p>Le web collecte l'OTP dans son propre écran, puis identity établit une session
 * interactive (cookie {@code AUTH_TX}). La suite reste le flux OIDC standard
 * Authorization Code + PKCE ({@code /oauth2/authorize} puis {@code /oauth2/token}).
 * Le login Google web passe par le flux OAuth2 navigateur standard.</p>
 */
@RestController
@RequestMapping("/api/auth/web")
public class WebAuthController {

    private static final Logger logger = LoggerFactory.getLogger(WebAuthController.class);

    private final PasswordlessAuthUseCase passwordlessAuthUseCase;
    private final WebSessionService webSessionService;
    private final AuthorizationRevocationService authorizationRevocationService;
    private final AccountDeletionCodeService accountDeletionCodeService;

    public WebAuthController(PasswordlessAuthUseCase passwordlessAuthUseCase,
                             WebSessionService webSessionService,
                             AuthorizationRevocationService authorizationRevocationService,
                             AccountDeletionCodeService accountDeletionCodeService) {
        this.passwordlessAuthUseCase = passwordlessAuthUseCase;
        this.webSessionService = webSessionService;
        this.authorizationRevocationService = authorizationRevocationService;
        this.accountDeletionCodeService = accountDeletionCodeService;
    }

    /**
     * Demande l'envoi d'un code de connexion. Répond toujours 202 (anti-énumération).
     */
    @PostMapping("/request-login-code")
    public ResponseEntity<Void> requestLoginCode(@Valid @RequestBody LoginCodeRequest request) {
        passwordlessAuthUseCase.requestCode(request.email());
        return ResponseEntity.accepted().build();
    }

    /**
     * Demande un code de confirmation de suppression de compte. Réponse toujours 202
     * (anti-énumération) ; un code n'est envoyé que pour un compte actif.
     */
    @PostMapping("/request-account-deletion-code")
    public ResponseEntity<Void> requestAccountDeletionCode(
            @Valid @RequestBody LoginCodeRequest request) {
        accountDeletionCodeService.requestCode(request.email());
        return ResponseEntity.accepted().build();
    }

    /**
     * Vérifie le code de suppression sans ouvrir de session. Le BFF enchaîne ensuite la
     * commande de suppression.
     */
    @PostMapping("/verify-account-deletion-code")
    public ResponseEntity<Void> verifyAccountDeletionCode(
            @Valid @RequestBody LoginWithCodeRequest request) {
        accountDeletionCodeService.verifyCode(request.email(), request.code());
        return ResponseEntity.noContent().build();
    }

    /**
     * Vérifie le code et établit la session web (cookie {@code AUTH_TX}).
     */
    @PostMapping("/login-with-code")
    public ResponseEntity<WebLoginResponse> loginWithCode(@Valid @RequestBody LoginWithCodeRequest request,
                                                          HttpServletRequest httpRequest,
                                                          HttpServletResponse httpResponse) {
        PasswordlessAuthUseCase.AuthenticatedUser user =
                passwordlessAuthUseCase.verifyCode(request.email(), request.code());
        webSessionService.establishSession(user, httpRequest, httpResponse);
        logger.info("Web session established via login code: userId={}", user.userId());
        return ResponseEntity.ok(new WebLoginResponse(user.userId(), user.email()));
    }

    /**
     * Démarre le login Google web : redirige vers le flux OAuth2 navigateur standard.
     * Le wrapper garde la surface d'authentification lisible côté client.
     */
    @GetMapping("/start-google-login")
    public ResponseEntity<Void> startGoogleLogin() {
        return ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create("/oauth2/authorization/google"))
                .build();
    }

    /**
     * Démarre le login Facebook web : redirige vers le flux OAuth2 navigateur standard.
     */
    @GetMapping("/start-facebook-login")
    public ResponseEntity<Void> startFacebookLogin() {
        return ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create("/oauth2/authorization/facebook"))
                .build();
    }

    /**
     * Ferme la session web et révoque l'autorisation OAuth2 si un refresh token est fourni.
     */
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@RequestBody(required = false) LogoutRequest request,
                                       HttpServletRequest httpRequest) {
        if (request != null && StringUtils.hasText(request.refreshToken())) {
            authorizationRevocationService.revoke(request.refreshToken());
        }
        HttpSession session = httpRequest.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        SecurityContextHolder.clearContext();
        return ResponseEntity.noContent().build();
    }
}
