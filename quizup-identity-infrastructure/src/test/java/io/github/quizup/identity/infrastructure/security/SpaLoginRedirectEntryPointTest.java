package io.github.quizup.identity.infrastructure.security;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.util.UriUtils;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class SpaLoginRedirectEntryPointTest {

    private final SpaLoginRedirectEntryPoint entryPoint =
            new SpaLoginRedirectEntryPoint("https://app.quizup.local/login");

    @Test
    void redirectsToSpaWithTheOriginalAuthorizationRequest() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/oauth2/authorize");
        request.setScheme("https");
        request.setServerPort(443);
        request.setServerName("identity.quizup.local");
        request.setQueryString("response_type=code&client_id=argocd&redirect_uri=https%3A%2F%2Fargocd.quizup.local%2Fauth%2Fcallback&state=abc");

        MockHttpServletResponse response = new MockHttpServletResponse();
        entryPoint.commence(request, response, new BadCredentialsException("unauthorized"));

        String location = response.getRedirectedUrl();
        assertThat(location).startsWith("https://app.quizup.local/login?continue=");

        String encoded = location.substring(location.indexOf("continue=") + "continue=".length());
        assertThat(UriUtils.decode(encoded, StandardCharsets.UTF_8))
                .isEqualTo("https://identity.quizup.local/oauth2/authorize"
                        + "?response_type=code&client_id=argocd"
                        + "&redirect_uri=https%3A%2F%2Fargocd.quizup.local%2Fauth%2Fcallback&state=abc");
    }

    @Test
    void redirectsWithoutQueryStringWhenThereIsNone() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/oauth2/authorize");
        request.setScheme("https");
        request.setServerPort(443);
        request.setServerName("identity.quizup.local");

        MockHttpServletResponse response = new MockHttpServletResponse();
        entryPoint.commence(request, response, new BadCredentialsException("unauthorized"));

        String encoded = response.getRedirectedUrl()
                .substring(response.getRedirectedUrl().indexOf("continue=") + "continue=".length());
        assertThat(UriUtils.decode(encoded, StandardCharsets.UTF_8))
                .isEqualTo("https://identity.quizup.local/oauth2/authorize");
    }
}
