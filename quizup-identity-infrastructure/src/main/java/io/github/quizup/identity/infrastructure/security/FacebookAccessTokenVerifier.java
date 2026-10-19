package io.github.quizup.identity.infrastructure.security;

import com.fasterxml.jackson.databind.JsonNode;
import io.github.quizup.identity.infrastructure.properties.AppProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.util.UriComponentsBuilder;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;

/**
 * Vérifie un access token Facebook émis par le login classique Android.
 *
 * <p>Contrairement à iOS (Limited Login OIDC), Android ne fournit pas d'ID token.
 * Le serveur valide donc l'access token auprès de Graph :
 * {@code /debug_token} (validité + app émettrice + user_id), puis {@code /me}
 * (email). L'App Secret reste côté serveur ; un {@code appsecret_proof} est ajouté
 * pour rester compatible avec le réglage « Require App Secret ».</p>
 */
@Component
public class FacebookAccessTokenVerifier {

    private static final Logger logger = LoggerFactory.getLogger(FacebookAccessTokenVerifier.class);

    private static final String GRAPH_API_BASE = "https://graph.facebook.com/v26.0";
    private static final String FACEBOOK_REGISTRATION_ID = "facebook";

    private final String appId;
    private final String appSecret;
    private final RestClient restClient;

    @Autowired
    public FacebookAccessTokenVerifier(AppProperties properties,
                                       ClientRegistrationRepository clientRegistrationRepository) {
        this(
                properties.auth().mobile().facebookAppId(),
                appSecret(clientRegistrationRepository),
                graphRestClientBuilder().build()
        );
    }

    /**
     * Client Graph : Facebook renvoie parfois ses erreurs en {@code text/javascript}
     * (notamment {@code /debug_token} pour un token invalide), pas seulement en
     * {@code application/json}. Sans ce converter, RestClient échoue à désérialiser et
     * l'erreur remonte en 503 au lieu de 401.
     */
    static RestClient.Builder graphRestClientBuilder() {
        MappingJackson2HttpMessageConverter json = new MappingJackson2HttpMessageConverter();
        json.setSupportedMediaTypes(List.of(
                MediaType.APPLICATION_JSON,
                MediaType.parseMediaType("application/javascript"),
                MediaType.parseMediaType("text/javascript")
        ));
        return RestClient.builder()
                .messageConverters(converters -> converters.add(0, json));
    }

    FacebookAccessTokenVerifier(String appId, String appSecret, RestClient restClient) {
        this.appId = appId;
        this.appSecret = appSecret;
        this.restClient = restClient;
    }

    /**
     * @throws FacebookAuthProblems.InvalidFacebookTokenProblem si le token est invalide
     * @throws FacebookAuthProblems.FacebookEmailMissingProblem si Facebook n'a pas fourni d'email
     * @throws FacebookAuthProblems.FacebookUnavailableProblem si Facebook est injoignable
     */
    public FacebookIdentity verify(String accessToken) {
        if (!StringUtils.hasText(appId) || !StringUtils.hasText(appSecret)) {
            throw new FacebookAuthProblems.FacebookUnavailableProblem(
                    new IllegalStateException("Facebook App ID or App Secret is not configured"));
        }
        if (!StringUtils.hasText(accessToken)) {
            throw new FacebookAuthProblems.InvalidFacebookTokenProblem(
                    new IllegalArgumentException("Missing access token"));
        }

        try {
            String subject = verifyToken(accessToken);
            String email = fetchEmail(accessToken, subject);
            return new FacebookIdentity(subject, email);
        } catch (HttpClientErrorException exception) {
            logger.debug("Facebook rejected the access token: status={}", exception.getStatusCode());
            throw new FacebookAuthProblems.InvalidFacebookTokenProblem(exception);
        } catch (RestClientException exception) {
            logger.warn("Facebook Graph API is unavailable: {}", exception.getMessage());
            throw new FacebookAuthProblems.FacebookUnavailableProblem(exception);
        }
    }

    private String verifyToken(String accessToken) {
        String debugUrl = UriComponentsBuilder
                .fromUriString(GRAPH_API_BASE + "/debug_token")
                .queryParam("input_token", accessToken)
                .queryParam("access_token", appId + "|" + appSecret)
                .build()
                .encode()
                .toUriString();

        JsonNode response = restClient.get()
                .uri(debugUrl)
                .retrieve()
                .body(JsonNode.class);
        JsonNode data = response == null ? null : response.path("data");
        if (data == null || !isValid(data.path("is_valid"))) {
            throw new FacebookAuthProblems.InvalidFacebookTokenProblem();
        }

        String tokenAppId = data.path("app_id").asText();
        if (!appId.equals(tokenAppId)) {
            throw new FacebookAuthProblems.InvalidFacebookTokenProblem(
                    new IllegalStateException("Facebook token was issued for another app"));
        }

        String subject = data.path("user_id").asText();
        if (!StringUtils.hasText(subject)) {
            throw new FacebookAuthProblems.InvalidFacebookTokenProblem();
        }
        return subject;
    }

    private String fetchEmail(String accessToken, String expectedSubject) {
        String profileUrl = UriComponentsBuilder
                .fromUriString(GRAPH_API_BASE + "/me")
                .queryParam("fields", "id,name,email")
                .queryParam("access_token", accessToken)
                .queryParam("appsecret_proof", appSecretProof(accessToken))
                .build()
                .encode()
                .toUriString();

        JsonNode profile = restClient.get()
                .uri(profileUrl)
                .retrieve()
                .body(JsonNode.class);
        if (profile == null) {
            throw new FacebookAuthProblems.InvalidFacebookTokenProblem();
        }

        String profileId = profile.path("id").asText();
        if (StringUtils.hasText(profileId) && !profileId.equals(expectedSubject)) {
            throw new FacebookAuthProblems.InvalidFacebookTokenProblem(
                    new IllegalStateException("Facebook profile does not match the token subject"));
        }

        String email = profile.path("email").asText(null);
        if (!StringUtils.hasText(email)) {
            throw new FacebookAuthProblems.FacebookEmailMissingProblem();
        }
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private String appSecretProof(String accessToken) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(appSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] digest = mac.doFinal(accessToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("Unable to compute appsecret_proof", exception);
        }
    }

    private static boolean isValid(JsonNode value) {
        if (value.isBoolean()) {
            return value.booleanValue();
        }
        return "true".equalsIgnoreCase(value.asText());
    }

    private static String appSecret(ClientRegistrationRepository repository) {
        if (repository == null) {
            return "";
        }
        ClientRegistration registration = repository.findByRegistrationId(FACEBOOK_REGISTRATION_ID);
        return registration == null ? "" : registration.getClientSecret();
    }
}
