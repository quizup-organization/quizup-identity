package io.github.quizup.identity.domain.model;

/**
 * Instantané du profil signalé au moment du dépôt : permet à l'administration de voir ce qui
 * était affiché même si le pseudonyme ou l'avatar ont changé depuis.
 */
public record UserReportSnapshot(
        String pseudonym,
        String avatarOptions
) {
}
