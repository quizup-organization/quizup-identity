package io.github.quizup.identity.infrastructure.security;

/**
 * Problèmes d'authentification Facebook natif.
 *
 * <p>iOS vérifie un ID token OIDC (Limited Login) ; Android fait vérifier son access
 * token par le serveur (Graph API). Ces exceptions sont traduites en
 * {@code ProblemDetail} par {@code AuthExceptionHandler}.</p>
 */
public final class FacebookAuthProblems {

    private FacebookAuthProblems() {
    }

    /** ID token ou access token Facebook invalide, expiré ou destiné à une autre app. */
    public static class InvalidFacebookTokenProblem extends RuntimeException {
        public InvalidFacebookTokenProblem() {
            super("Invalid Facebook token");
        }

        public InvalidFacebookTokenProblem(Throwable cause) {
            super("Invalid Facebook token", cause);
        }
    }

    /** Facebook n'a pas transmis d'email : le compte QuizUp ne peut pas être résolu. */
    public static class FacebookEmailMissingProblem extends RuntimeException {
        public FacebookEmailMissingProblem() {
            super("Facebook did not provide an email");
        }
    }

    /** Les services Facebook sont momentanément inaccessibles. */
    public static class FacebookUnavailableProblem extends RuntimeException {
        public FacebookUnavailableProblem(Throwable cause) {
            super("Facebook identity provider is unavailable", cause);
        }
    }
}
