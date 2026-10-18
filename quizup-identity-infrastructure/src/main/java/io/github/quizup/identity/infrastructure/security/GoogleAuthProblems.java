package io.github.quizup.identity.infrastructure.security;

/**
 * Problèmes d'authentification Google natif.
 *
 * <p>Le token Google est fourni par le SDK mobile ; identity le vérifie localement
 * (JWKS Google) avant d'émettre les tokens QuizUp. Ces exceptions sont traduites en
 * {@code ProblemDetail} par {@code AuthExceptionHandler}.</p>
 */
public final class GoogleAuthProblems {

    private GoogleAuthProblems() {
    }

    /** ID token Google invalide, expiré ou destiné à un autre client. */
    public static class InvalidGoogleTokenProblem extends RuntimeException {
        public InvalidGoogleTokenProblem() {
            super("Invalid Google ID token");
        }

        public InvalidGoogleTokenProblem(Throwable cause) {
            super("Invalid Google ID token", cause);
        }
    }

    /** Les clés publiques Google sont momentanément inaccessibles. */
    public static class GoogleUnavailableProblem extends RuntimeException {
        public GoogleUnavailableProblem(Throwable cause) {
            super("Google identity provider is unavailable", cause);
        }
    }
}
