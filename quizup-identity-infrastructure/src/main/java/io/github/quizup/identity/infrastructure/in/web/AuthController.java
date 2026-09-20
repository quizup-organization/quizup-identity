package io.github.quizup.identity.infrastructure.in.web;

import io.github.quizup.identity.domain.port.in.CheckUserUseCase;
import io.github.quizup.identity.domain.port.in.RegisterUserUseCase;
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
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

/**
 * API JSON d'authentification pilotée par la SPA/mobile.
 * <p>
 * Ce n'est <b>pas</b> un Password Grant : l'endpoint établit une session
 * interactive temporaire (Spring Session JDBC, cookie {@code AUTH_TX}) puis la
 * SPA poursuit le pipeline standard Authorization Code + PKCE. Aucun JWT n'est
 * émis ici.
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    /** Attente bornée de la projection read-model après inscription. */
    private static final int PROJECTION_MAX_ATTEMPTS = 20;
    private static final long PROJECTION_RETRY_DELAY_MS = 100L;

    private final AuthenticationManager authenticationManager;
    private final SecurityContextRepository securityContextRepository;
    private final RegisterUserUseCase registerUserUseCase;
    private final CheckUserUseCase checkUserUseCase;

    public AuthController(AuthenticationManager authenticationManager,
                          SecurityContextRepository securityContextRepository,
                          RegisterUserUseCase registerUserUseCase,
                          CheckUserUseCase checkUserUseCase) {
        this.authenticationManager = authenticationManager;
        this.securityContextRepository = securityContextRepository;
        this.registerUserUseCase = registerUserUseCase;
        this.checkUserUseCase = checkUserUseCase;
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request,
                                              HttpServletRequest httpRequest,
                                              HttpServletResponse httpResponse) {
        Authentication authentication = authenticate(request.email(), request.password());
        establishSession(authentication, httpRequest, httpResponse);
        return ResponseEntity.ok(toResponse(authentication));
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request,
                                                 HttpServletRequest httpRequest,
                                                 HttpServletResponse httpResponse) {
        String userId = UUID.randomUUID().toString();
        join(registerUserUseCase.registerWithPassword(userId, request.email(), request.password()));

        awaitProjection(request.email());

        Authentication authentication = authenticate(request.email(), request.password());
        establishSession(authentication, httpRequest, httpResponse);
        logger.info("User registered and session established: userId={}", userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(authentication));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest httpRequest) {
        HttpSession session = httpRequest.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        SecurityContextHolder.clearContext();
        return ResponseEntity.noContent().build();
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ProblemDetail> handleAuthenticationException(AuthenticationException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, "Email ou mot de passe incorrect");
        problem.setTitle("Unauthorized");
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(problem);
    }

    private Authentication authenticate(String email, String password) {
        return authenticationManager.authenticate(
                UsernamePasswordAuthenticationToken.unauthenticated(email, password));
    }

    private void establishSession(Authentication authentication,
                                  HttpServletRequest httpRequest,
                                  HttpServletResponse httpResponse) {
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
        securityContextRepository.saveContext(context, httpRequest, httpResponse);
    }

    private AuthResponse toResponse(Authentication authentication) {
        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        return new AuthResponse(principal.getUserId(), principal.getEmail());
    }

    private void awaitProjection(String email) {
        for (int attempt = 0; attempt < PROJECTION_MAX_ATTEMPTS; attempt++) {
            if (Boolean.TRUE.equals(checkUserUseCase.existsByEmail(email).join())) {
                return;
            }
            try {
                Thread.sleep(PROJECTION_RETRY_DELAY_MS);
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
                return;
            }
        }
        logger.warn("User projection still not visible after registration: email={}", email);
    }

    private void join(CompletableFuture<?> future) {
        try {
            future.join();
        } catch (CompletionException exception) {
            if (exception.getCause() instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }
            throw exception;
        }
    }
}
