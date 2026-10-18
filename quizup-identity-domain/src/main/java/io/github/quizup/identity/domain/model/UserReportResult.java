package io.github.quizup.identity.domain.model;

/**
 * Résultat idempotent du dépôt d'un signalement.
 *
 * @param reportId identifiant du signalement ouvert (nouveau ou déjà existant)
 * @param created  vrai si un nouvel événement a été émis
 */
public record UserReportResult(String reportId, boolean created) {
}
