package io.github.quizup.identity.infrastructure.in.web;

import io.github.quizup.identity.domain.exception.UserProblems;
import io.github.quizup.identity.domain.exception.UserReportProblems;
import io.github.quizup.identity.infrastructure.security.FacebookAuthProblems;
import io.github.quizup.identity.infrastructure.security.GoogleAuthProblems;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;

/**
 * Traduction des erreurs d'authentification en {@code ProblemDetail} lisibles côté client.
 *
 * <p>Les clients affichent {@code detail} tel quel ; {@code type} reste stable pour un
 * éventuel traitement programmatique.</p>
 */
@RestControllerAdvice(assignableTypes = {WebAuthController.class, MobileAuthController.class})
public class AuthExceptionHandler {

    @ExceptionHandler(UserProblems.InvalidLoginCodeProblem.class)
    public ResponseEntity<ProblemDetail> handleInvalidLoginCode(UserProblems.InvalidLoginCodeProblem exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.UNAUTHORIZED,
                "Le code est invalide, expiré ou déjà utilisé. Demande un nouveau code."
        );
        problem.setType(URI.create(exception.getType()));
        problem.setTitle("Code de connexion invalide");
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(problem);
    }

    @ExceptionHandler(UserProblems.InvalidAccountDeletionCodeProblem.class)
    public ResponseEntity<ProblemDetail> handleInvalidAccountDeletionCode(
            UserProblems.InvalidAccountDeletionCodeProblem exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST,
                "Le code de suppression est invalide, expiré ou déjà utilisé."
        );
        problem.setType(URI.create(exception.getType()));
        problem.setTitle("Code de suppression invalide");
        return ResponseEntity.badRequest().body(problem);
    }

    @ExceptionHandler(UserReportProblems.AccountSuspendedProblem.class)
    public ResponseEntity<ProblemDetail> handleAccountSuspended(UserReportProblems.AccountSuspendedProblem exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.FORBIDDEN,
                "Ce compte est suspendu. Consulte l'email reçu pour connaître le motif et l'échéance."
        );
        problem.setType(URI.create(exception.getType()));
        problem.setTitle("Compte suspendu");
        Object suspendedUntil = exception.getContext() == null
                ? null
                : exception.getContext().get("suspendedUntil");
        if (suspendedUntil != null && !suspendedUntil.toString().isBlank()) {
            problem.setProperty("suspendedUntil", suspendedUntil.toString());
        }
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(problem);
    }

    /**
     * Filet de sécurité du {@code SuspendedAccountTokenGenerator} : si le compte est suspendu
     * entre la vérification du login et l'émission des tokens, le générateur lève une
     * {@link OAuth2AuthenticationException} {@code account_suspended}.
     */
    @ExceptionHandler(OAuth2AuthenticationException.class)
    public ResponseEntity<ProblemDetail> handleOAuth2Authentication(
            OAuth2AuthenticationException exception) {
        OAuth2Error error = exception.getError();
        boolean suspended = "account_suspended".equals(error.getErrorCode())
                || "account_suspended".equals(error.getDescription());

        ProblemDetail problem = suspended
                ? ProblemDetail.forStatusAndDetail(
                        HttpStatus.FORBIDDEN,
                        "Ce compte est suspendu. Consulte l'email reçu pour connaître le motif et l'échéance.")
                : ProblemDetail.forStatusAndDetail(
                        HttpStatus.UNAUTHORIZED,
                        "Connexion impossible. Réessaie.");
        problem.setType(URI.create(suspended
                ? "urn:quizup:identity:accountSuspended"
                : "urn:quizup:auth:oauth2Error"));
        problem.setTitle(suspended ? "Compte suspendu" : "Connexion refusée");
        return ResponseEntity.status(problem.getStatus()).body(problem);
    }

    @ExceptionHandler(GoogleAuthProblems.InvalidGoogleTokenProblem.class)
    public ResponseEntity<ProblemDetail> handleInvalidGoogleToken(
            GoogleAuthProblems.InvalidGoogleTokenProblem exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.UNAUTHORIZED,
                "Le jeton Google est invalide ou expiré. Réessaie."
        );
        problem.setType(URI.create("urn:quizup:auth:invalidGoogleToken"));
        problem.setTitle("Connexion Google refusée");
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(problem);
    }

    @ExceptionHandler(GoogleAuthProblems.GoogleUnavailableProblem.class)
    public ResponseEntity<ProblemDetail> handleGoogleUnavailable(
            GoogleAuthProblems.GoogleUnavailableProblem exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.SERVICE_UNAVAILABLE,
                "Le service Google est temporairement indisponible. Réessaie plus tard."
        );
        problem.setType(URI.create("urn:quizup:auth:googleUnavailable"));
        problem.setTitle("Google indisponible");
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(problem);
    }

    @ExceptionHandler(FacebookAuthProblems.InvalidFacebookTokenProblem.class)
    public ResponseEntity<ProblemDetail> handleInvalidFacebookToken(
            FacebookAuthProblems.InvalidFacebookTokenProblem exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.UNAUTHORIZED,
                "Le jeton Facebook est invalide ou expiré. Réessaie."
        );
        problem.setType(URI.create("urn:quizup:auth:invalidFacebookToken"));
        problem.setTitle("Connexion Facebook refusée");
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(problem);
    }

    @ExceptionHandler(FacebookAuthProblems.FacebookEmailMissingProblem.class)
    public ResponseEntity<ProblemDetail> handleFacebookEmailMissing(
            FacebookAuthProblems.FacebookEmailMissingProblem exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.UNPROCESSABLE_ENTITY,
                "Facebook n'a pas transmis d'email. Utilise la connexion par email."
        );
        problem.setType(URI.create("urn:quizup:auth:facebookEmailMissing"));
        problem.setTitle("Email Facebook manquant");
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(problem);
    }

    @ExceptionHandler(FacebookAuthProblems.FacebookUnavailableProblem.class)
    public ResponseEntity<ProblemDetail> handleFacebookUnavailable(
            FacebookAuthProblems.FacebookUnavailableProblem exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.SERVICE_UNAVAILABLE,
                "Le service Facebook est temporairement indisponible. Réessaie ou utilise la connexion par email."
        );
        problem.setType(URI.create("urn:quizup:auth:facebookUnavailable"));
        problem.setTitle("Facebook indisponible");
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(problem);
    }
}
