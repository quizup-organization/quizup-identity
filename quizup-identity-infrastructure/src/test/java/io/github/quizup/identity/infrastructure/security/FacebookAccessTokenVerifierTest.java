package io.github.quizup.identity.infrastructure.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.queryParam;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class FacebookAccessTokenVerifierTest {

    private static final String APP_ID = "2122240041993058";
    private static final String APP_SECRET = "app-secret";
    private static final String ACCESS_TOKEN = "facebook-access-token";

    private MockRestServiceServer server;
    private FacebookAccessTokenVerifier verifier;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        verifier = new FacebookAccessTokenVerifier(APP_ID, APP_SECRET, builder.build());
    }

    @Test
    void verifiesValidTokenAndNormalizesEmail() {
        server.expect(requestTo(org.hamcrest.Matchers.startsWith(
                        "https://graph.facebook.com/v26.0/debug_token")))
                .andExpect(queryParam("input_token", ACCESS_TOKEN))
                .andRespond(withSuccess("""
                        {"data":{"app_id":"2122240041993058","is_valid":true,"user_id":"fb-user-1"}}
                        """, MediaType.APPLICATION_JSON));

        server.expect(requestTo(org.hamcrest.Matchers.startsWith(
                        "https://graph.facebook.com/v26.0/me")))
                .andExpect(queryParam("fields", "id,name,email"))
                .andExpect(queryParam("access_token", ACCESS_TOKEN))
                .andRespond(withSuccess("""
                        {"id":"fb-user-1","name":"Player","email":"  Player@Example.COM "}
                        """, MediaType.APPLICATION_JSON));

        FacebookIdentity identity = verifier.verify(ACCESS_TOKEN);

        assertThat(identity.subject()).isEqualTo("fb-user-1");
        assertThat(identity.email()).isEqualTo("player@example.com");
        server.verify();
    }

    @Test
    void rejectsInvalidToken() {
        server.expect(requestTo(org.hamcrest.Matchers.startsWith(
                        "https://graph.facebook.com/v26.0/debug_token")))
                .andRespond(withSuccess("""
                        {"data":{"app_id":"2122240041993058","is_valid":false,"user_id":"fb-user-1"}}
                        """, MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> verifier.verify(ACCESS_TOKEN))
                .isInstanceOf(FacebookAuthProblems.InvalidFacebookTokenProblem.class);
    }

    @Test
    void rejectsTokenIssuedForAnotherApp() {
        server.expect(requestTo(org.hamcrest.Matchers.startsWith(
                        "https://graph.facebook.com/v26.0/debug_token")))
                .andRespond(withSuccess("""
                        {"data":{"app_id":"another-app","is_valid":true,"user_id":"fb-user-1"}}
                        """, MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> verifier.verify(ACCESS_TOKEN))
                .isInstanceOf(FacebookAuthProblems.InvalidFacebookTokenProblem.class);
    }

    @Test
    void rejectsMissingEmail() {
        server.expect(requestTo(org.hamcrest.Matchers.startsWith(
                        "https://graph.facebook.com/v26.0/debug_token")))
                .andRespond(withSuccess("""
                        {"data":{"app_id":"2122240041993058","is_valid":true,"user_id":"fb-user-1"}}
                        """, MediaType.APPLICATION_JSON));

        server.expect(requestTo(org.hamcrest.Matchers.startsWith(
                        "https://graph.facebook.com/v26.0/me")))
                .andRespond(withSuccess("""
                        {"id":"fb-user-1","name":"Player"}
                        """, MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> verifier.verify(ACCESS_TOKEN))
                .isInstanceOf(FacebookAuthProblems.FacebookEmailMissingProblem.class);
    }

    @Test
    void mapsGraphClientErrorToInvalidToken() {
        server.expect(requestTo(org.hamcrest.Matchers.startsWith(
                        "https://graph.facebook.com/v26.0/debug_token")))
                .andRespond(withStatus(HttpStatus.BAD_REQUEST)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("""
                                {"error":{"message":"Invalid OAuth access token"}}
                                """));

        assertThatThrownBy(() -> verifier.verify(ACCESS_TOKEN))
                .isInstanceOf(FacebookAuthProblems.InvalidFacebookTokenProblem.class);
    }

    @Test
    void mapsGraphServerErrorToUnavailable() {
        server.expect(requestTo(org.hamcrest.Matchers.startsWith(
                        "https://graph.facebook.com/v26.0/debug_token")))
                .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));

        assertThatThrownBy(() -> verifier.verify(ACCESS_TOKEN))
                .isInstanceOf(FacebookAuthProblems.FacebookUnavailableProblem.class);
    }
}
