package io.github.quizup.identity.domain.port.in;

/**
 * Port entrant - Authentification sans mot de passe par code à usage unique (OTP email).
 */
public interface PasswordlessAuthUseCase {

    /**
     * Demande l'envoi d'un code de connexion. Ne révèle jamais l'existence du compte :
     * l'appel réussit toujours, un email n'est envoyé que pour les comptes éligibles.
     *
     * @param email email de l'utilisateur
     */
    void requestCode(String email);

    /**
     * Vérifie le code et retourne l'utilisateur authentifié (créé au premier login).
     *
     * @param email email de l'utilisateur
     * @param code  code reçu par email
     * @return l'utilisateur authentifié
     */
    AuthenticatedUser verifyCode(String email, String code);

    record AuthenticatedUser(String userId, String email) {
    }
}
