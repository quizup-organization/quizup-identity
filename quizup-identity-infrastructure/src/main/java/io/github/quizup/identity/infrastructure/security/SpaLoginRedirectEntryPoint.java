package io.github.quizup.identity.infrastructure.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.DefaultRedirectStrategy;
import org.springframework.security.web.RedirectStrategy;
import org.springframework.web.util.UriUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Entry point OIDC : redirige le navigateur vers la SPA de connexion en conservant
 * l'URL d'autorisation d'origine dans le paramètre {@code continue}.
 *
 * <p>Sans cela, un client tiers (Grafana, ArgoCD, mobile…) déclenchant une
 * autorisation sans session SPA serait abandonné après authentification : la SPA
 * ne connaît pas le client d'origine. La SPA reprend la cible après login.</p>
 */
public class SpaLoginRedirectEntryPoint implements AuthenticationEntryPoint {

    static final String CONTINUE_PARAMETER = "continue";

    private final String loginPageUri;
    private final RedirectStrategy redirectStrategy = new DefaultRedirectStrategy();

    public SpaLoginRedirectEntryPoint(String loginPageUri) {
        this.loginPageUri = loginPageUri;
    }

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        String originalUrl = request.getRequestURL().toString();
        if (request.getQueryString() != null) {
            originalUrl = originalUrl + "?" + request.getQueryString();
        }

        String separator = loginPageUri.contains("?") ? "&" : "?";
        String target = loginPageUri + separator + CONTINUE_PARAMETER + "="
                + UriUtils.encodeQueryParam(originalUrl, StandardCharsets.UTF_8);

        redirectStrategy.sendRedirect(request, response, target);
    }
}
