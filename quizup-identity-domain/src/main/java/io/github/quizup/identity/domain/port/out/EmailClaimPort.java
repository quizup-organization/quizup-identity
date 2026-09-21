package io.github.quizup.identity.domain.port.out;

/**
 * Port sortant - réservation atomique d'un email pour un utilisateur.
 *
 * <p>Contrairement à la lecture de la projection {@code user_entry} (éventuellement cohérente),
 * cette réservation s'appuie sur une contrainte unique en base : deux inscriptions concurrentes
 * pour le même email ne peuvent pas aboutir (la seconde est rejetée).</p>
 */
public interface EmailClaimPort {

    /**
     * Réserve l'email pour ce {@code userId}.
     *
     * @return {@code true} si la réservation a réussi (email libre), {@code false} s'il est déjà pris.
     */
    boolean claim(String email, String userId);
}
