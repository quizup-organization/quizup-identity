package io.github.quizup.identity.domain.model;

/**
 * Filtres de la file de signalements admin.
 *
 * @param status    statut requis (au moins un signalement de ce statut) ; {@code null} = tous
 * @param suspended filtre sur le statut effectif du compte ; {@code null} = tous
 */
public record UserReportSummaryFilter(
        UserReportStatus status,
        Boolean suspended
) {
}
