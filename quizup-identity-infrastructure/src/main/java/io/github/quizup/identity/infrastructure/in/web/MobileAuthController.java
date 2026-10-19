package io.github.quizup.identity.infrastructure.in.web;

import io.github.quizup.identity.application.service.SocialUserService;
import io.github.quizup.identity.domain.model.SocialProvider;
import io.github.quizup.identity.domain.model.User;
import io.github.quizup.identity.domain.port.in.PasswordlessAuthUseCase;
import io.github.quizup.identity.infrastructure.properties.AppProperties;
import io.github.quizup.identity.infrastructure.security.AuthorizationRevocationService;
import io.github.quizup.identity.infrastructure.security.FacebookAccessTokenVerifier;
import io.github.quizup.identity.infrastructure.security.FacebookIdTokenVerifier;
import io.github.quizup.identity.infrastructure.security.FacebookIdentity;
import io.github.quizup.identity.infrastructure.security.GoogleIdTokenVerifier;
import io.github.quizup.identity.infrastructure.security.NativeTokenIssuer;
import io.github.quizup.identity.infrastructure.security.Roles;
import io.github.quizup.identity.infrastructure.security.UserPrincipal;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Authentification native mobile : OTP email ou ID token Google.
 *
 * <p>Les endpoints retournent directement les tokens OAuth2 (access JWT + refresh) :
 * pas de navigateur, pas de cookie. Le refresh utilise ensuite le endpoint standard
 * {@code /oauth2/token}.</p>
 */
@RestController
@RequestMapping("/api/auth/mobile")
public class MobileAuthController {

    private static final Logger logger = LoggerFactory.getLogger(MobileAuthController.class);

    private final PasswordlessAuthUseCase passwordlessAuthUseCase;
    private final SocialUserService socialUserService;
    private final GoogleIdTokenVerifier googleIdTokenVerifier;
    private final FacebookIdTokenVerifier facebookIdTokenVerifier;
    private final FacebookAccessTokenVerifier facebookAccessTokenVerifier;
    private final NativeTokenIssuer nativeTokenIssuer;
    private final AuthorizationRevocationService authorizationRevocationService;
    private final Roles roles;
    private final AppProperties properties;

    public MobileAuthController(PasswordlessAuthUseCase passwordlessAuthUseCase,
                                SocialUserService socialUserService,
                                GoogleIdTokenVerifier googleIdTokenVerifier,
                                FacebookIdTokenVerifier facebookIdTokenVerifier,
                                FacebookAccessTokenVerifier facebookAccessTokenVerifier,
                                NativeTokenIssuer nativeTokenIssuer,
                                AuthorizationRevocationService authorizationRevocationService,
                                Roles roles,
                                AppProperties properties) {
        this.passwordlessAuthUseCase = passwordlessAuthUseCase;
        this.socialUserService = socialUserService;
        this.googleIdTokenVerifier = googleIdTokenVerifier;
        this.facebookIdTokenVerifier = facebookIdTokenVerifier;
        this.facebookAccessTokenVerifier = facebookAccessTokenVerifier;
        this.nativeTokenIssuer = nativeTokenIssuer;
        this.authorizationRevocationService = authorizationRevocationService;
        this.roles = roles;
        this.properties = properties;
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
     * Vérifie le code OTP et retourne une session mobile (access + refresh tokens).
     */
    @PostMapping("/login-with-code")
    public ResponseEntity<MobileLoginResponse> loginWithCode(
            @Valid @RequestBody LoginWithCodeRequest request) {
        PasswordlessAuthUseCase.AuthenticatedUser user =
                passwordlessAuthUseCase.verifyCode(request.email(), request.code());
        logger.info("Mobile session established via login code: userId={}", user.userId());
        return tokenResponse(user.userId(), user.email());
    }

    /**
     * Vérifie l'ID token Google du SDK natif et retourne une session mobile.
     */
    @PostMapping("/login-with-google")
    public ResponseEntity<MobileLoginResponse> loginWithGoogle(
            @Valid @RequestBody LoginWithGoogleRequest request) {
        GoogleIdTokenVerifier.GoogleIdentity identity =
                googleIdTokenVerifier.verify(request.idToken());
        User user = socialUserService.resolveOrCreate(identity.email(), SocialProvider.GOOGLE);
        logger.info("Mobile session established via Google: userId={}", user.userId());
        return tokenResponse(user.userId(), user.email());
    }

    /**
     * Vérifie l'identité Facebook native et retourne une session mobile.
     *
     * <p>iOS (Limited Login) fournit un ID token OIDC + nonce ; Android (login
     * classique) fournit un access token vérifié auprès de Graph.</p>
     */
    @PostMapping("/login-with-facebook")
    public ResponseEntity<MobileLoginResponse> loginWithFacebook(
            @Valid @RequestBody LoginWithFacebookRequest request) {
        FacebookIdentity identity = StringUtils.hasText(request.idToken())
                ? facebookIdTokenVerifier.verify(request.idToken(), request.nonce())
                : facebookAccessTokenVerifier.verify(request.accessToken());
        User user = socialUserService.resolveOrCreate(identity.email(), SocialProvider.FACEBOOK);
        logger.info("Mobile session established via Facebook: userId={}", user.userId());
        return tokenResponse(user.userId(), user.email());
    }

    /**
     * Révoque l'autorisation OAuth2 associée au refresh token (pas de cookie mobile).
     */
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@RequestBody(required = false) LogoutRequest request) {
        if (request != null && StringUtils.hasText(request.refreshToken())) {
            authorizationRevocationService.revoke(request.refreshToken());
        }
        return ResponseEntity.noContent().build();
    }

    private ResponseEntity<MobileLoginResponse> tokenResponse(String userId, String email) {
        UserPrincipal principal = new UserPrincipal(
                userId,
                email,
                roles.forUser(userId, email)
        );
        Authentication authentication = UsernamePasswordAuthenticationToken.authenticated(
                principal, null, principal.getAuthorities());

        NativeTokenIssuer.IssuedTokens tokens = nativeTokenIssuer.issue(
                authentication,
                properties.auth().mobile().clientId()
        );

        MobileLoginResponse response = new MobileLoginResponse(
                userId,
                email,
                tokens.accessToken(),
                tokens.refreshToken(),
                tokens.tokenType(),
                tokens.expiresIn()
        );
        return ResponseEntity.ok()
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .header(HttpHeaders.PRAGMA, "no-cache")
                .body(response);
    }
}
