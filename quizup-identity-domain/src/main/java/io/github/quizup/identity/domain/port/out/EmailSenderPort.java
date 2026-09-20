package io.github.quizup.identity.domain.port.out;

import java.time.Duration;

/**
 * Port sortant - Envoi d'emails transactionnels.
 */
public interface EmailSenderPort {

    /**
     * Envoie un code de connexion à usage unique par email.
     *
     * @param email destinataire
     * @param code  code en clair (jamais loggé)
     * @param ttl   durée de validité du code
     */
    void sendLoginCode(String email, String code, Duration ttl);
}
