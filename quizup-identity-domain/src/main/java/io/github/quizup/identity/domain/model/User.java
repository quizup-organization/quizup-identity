package io.github.quizup.identity.domain.model;

import java.time.Instant;
import java.util.Set;

/**
 * Modèle domaine représentant un utilisateur tel que vu par le domaine.
 * N'importe AUCUN type JPA, Spring, ou framework.
 * Immuable par convention (record).
 */
public record User(
        String userId,
        String email,
        String password,
        Set<SocialProvider> linkedSocialAccounts,
        Instant createdAt
) {

    /**
     * Vérifie si cet utilisateur peut se connecter par mot de passe.
     */
    public boolean hasPassword() {
        return password == null || password.isBlank();
    }
}

