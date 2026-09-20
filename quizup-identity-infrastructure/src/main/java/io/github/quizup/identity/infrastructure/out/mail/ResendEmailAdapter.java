package io.github.quizup.identity.infrastructure.out.mail;

import io.github.quizup.identity.domain.port.out.EmailSenderPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.List;
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

    public ResendEmailAdapter(
            @Value("${app.mail.api-key:}") String apiKey,
            @Value("${app.mail.from:QuizUp <no-reply@quizup.cnadjim.fr>}") String from,
            @Value("${app.mail.base-url:https://api.resend.com}") String baseUrl) {
        this.apiKey = apiKey;
        this.from = from;
        this.baseUrl = baseUrl;
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
}
