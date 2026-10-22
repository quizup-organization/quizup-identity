package io.github.quizup.identity.infrastructure.out.mail;

import io.github.quizup.identity.domain.model.UserSuspensionReason;
import io.github.quizup.identity.domain.port.out.EmailSenderPort;
import io.github.quizup.identity.infrastructure.properties.AppProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Adapter d'envoi d'emails transactionnels via l'API Resend.
 *
 * <p>Si aucune clé API n'est configurée (profil local/test), le code est journalisé
 * au lieu d'être envoyé : cela permet le développement local et les tests.</p>
 */
@Component
public class ResendEmailAdapter implements EmailSenderPort {

    private static final Logger logger = LoggerFactory.getLogger(ResendEmailAdapter.class);

    private final String apiKey;
    private final String from;
    private final String baseUrl;
    private final RestClient restClient;

    public ResendEmailAdapter(AppProperties properties) {
        this.apiKey = properties.mail().apiKey();
        this.from = properties.mail().from();
        this.baseUrl = properties.mail().baseUrl();
        this.restClient = RestClient.create();
    }

    @Override
    public void sendLoginCode(String email, String code, Duration ttl) {
        if (!StringUtils.hasText(apiKey)) {
            logger.warn("Mail API key not configured - login code for {} is: {}", email, code);
            return;
        }

        long minutes = Math.max(1, ttl.toMinutes());

        Map<String, Object> body = Map.of(
                "from", from,
                "to", List.of(email),
                "subject", "Votre code de connexion QuizUp",
                "html", renderHtml(code, minutes)
        );

        restClient.post()
                .uri(baseUrl + "/emails")
                .header("Authorization", "Bearer " + apiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .toBodilessEntity();

        logger.info("Login code sent to {}", email);
    }

    @Override
    public void sendAccountDeletionCode(String email, String code, Duration ttl) {
        if (!StringUtils.hasText(apiKey)) {
            logger.warn("Mail API key not configured - account deletion code for {} is: {}", email, code);
            return;
        }

        long minutes = Math.max(1, ttl.toMinutes());
        Map<String, Object> body = Map.of(
                "from", from,
                "to", List.of(email),
                "subject", "Confirmation de suppression de ton compte QuizUp",
                "html", renderDeletionCodeHtml(code, minutes)
        );

        restClient.post()
                .uri(baseUrl + "/emails")
                .header("Authorization", "Bearer " + apiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .toBodilessEntity();

        logger.info("Account deletion code sent to {}", email);
    }

    @Override
    public void sendAccountSuspended(String email, UserSuspensionReason reason, Instant suspendedUntil) {
        String reasonLabel = reasonLabel(reason);
        String untilLabel = suspendedUntil == null
                ? "définitivement"
                : "jusqu'au " + DATE_FORMAT.format(suspendedUntil);

        if (!StringUtils.hasText(apiKey)) {
            logger.warn("Mail API key not configured - suspension notice for {}: {} ({})",
                    email, reasonLabel, untilLabel);
            return;
        }

        Map<String, Object> body = Map.of(
                "from", from,
                "to", List.of(email),
                "subject", "Ton compte QuizUp a été suspendu",
                "html", renderSuspensionHtml(reasonLabel, untilLabel)
        );

        restClient.post()
                .uri(baseUrl + "/emails")
                .header("Authorization", "Bearer " + apiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .toBodilessEntity();

        logger.info("Suspension notice sent to {}", email);
    }

    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("dd/MM/yyyy 'à' HH:mm", Locale.FRENCH)
                    .withZone(ZoneId.of("Europe/Paris"));

    private String renderHtml(String code, long minutes) {
        return """
                <div style="font-family:Arial,sans-serif;max-width:480px;margin:0 auto">
                  <h2>Connexion QuizUp</h2>
                  <p>Voici votre code de connexion :</p>
                  <p style="font-size:32px;font-weight:bold;letter-spacing:6px">%s</p>
                  <p>Ce code expire dans %d minutes. Si vous n'êtes pas à l'origine de cette
                  demande, ignorez cet email.</p>
                </div>
                """.formatted(code, minutes);
    }

    private String renderDeletionCodeHtml(String code, long minutes) {
        return """
                <div style="font-family:Arial,sans-serif;max-width:480px;margin:0 auto">
                  <h2>Suppression de ton compte QuizUp</h2>
                  <p>Voici ton code de confirmation :</p>
                  <p style="font-size:32px;font-weight:bold;letter-spacing:6px">%s</p>
                  <p>Ce code expire dans %d minutes. Si tu n'as pas demandé cette suppression,
                  ignore cet email : ton compte reste actif.</p>
                </div>
                """.formatted(code, minutes);
    }

    private String renderSuspensionHtml(String reasonLabel, String untilLabel) {
        return """
                <div style="font-family:Arial,sans-serif;max-width:480px;margin:0 auto">
                  <h2>Compte suspendu</h2>
                  <p>Ton compte QuizUp est suspendu %s.</p>
                  <p><strong>Motif :</strong> %s</p>
                  <p>Si tu penses qu'il s'agit d'une erreur, tu peux répondre à cet email.</p>
                </div>
                """.formatted(untilLabel, reasonLabel);
    }

    private static String reasonLabel(UserSuspensionReason reason) {
        if (reason == null) {
            return "non précisé";
        }
        return switch (reason) {
            case HARASSMENT -> "harcèlement";
            case HATE_SPEECH -> "discours haineux";
            case THREATS -> "menaces ou violence";
            case IMPERSONATION -> "usurpation d'identité";
            case PRIVACY_VIOLATION -> "atteinte à la vie privée";
            case INAPPROPRIATE_CONTENT -> "contenu inapproprié";
            case SPAM -> "spam";
            case COPYRIGHT -> "atteinte au droit d'auteur";
            case CHEATING -> "triche";
            case EXPLOIT_ABUSE -> "exploitation de faille";
            case MULTIPLE_ACCOUNTS -> "comptes multiples ou contournement";
            case TERMS_VIOLATION -> "violation des conditions d'utilisation";
            case MINOR_SAFETY -> "mise en danger de mineurs";
            case FRAUD -> "fraude ou arnaque";
            case OTHER -> "autre";
        };
    }
}
