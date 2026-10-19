package io.github.quizup.identity.infrastructure.security;

import io.github.quizup.identity.application.service.SocialUserService;
import io.github.quizup.identity.domain.exception.UserReportProblems;
import io.github.quizup.identity.domain.model.SocialProvider;
import io.github.quizup.identity.domain.model.User;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class FacebookOAuth2UserServiceTest {

    private final SocialUserService socialUserService = mock(SocialUserService.class);
    private final Roles roles = mock(Roles.class);
    private final FacebookOAuth2UserService service =
            new FacebookOAuth2UserService(socialUserService, roles);

    @Test
    void rejectsMissingEmailWithoutResolvingUser() {
        DefaultOAuth2User facebookUser = new DefaultOAuth2User(
                List.of(new SimpleGrantedAuthority("ROLE_USER")),
                Map.of("id", "fb-user-1", "name", "Player"),
                "id");

        assertThatThrownBy(() -> service.resolve(facebookUser))
                .isInstanceOfSatisfying(OAuth2AuthenticationException.class, exception ->
                        assertThat(exception.getError().getErrorCode())
                                .isEqualTo(FacebookOAuth2UserService.EMAIL_MISSING_ERROR_CODE));
        verifyNoInteractions(socialUserService);
    }

    @Test
    void resolvesUserFromFacebookEmail() {
        User user = new User("user-1", "player@example.com", Set.of(SocialProvider.FACEBOOK), Instant.now());
        when(socialUserService.resolveOrCreate(" Player@Example.COM ", SocialProvider.FACEBOOK))
                .thenReturn(user);
        when(roles.forUser("user-1", "player@example.com"))
                .thenReturn(List.of(new SimpleGrantedAuthority("ROLE_USER")));

        DefaultOAuth2User facebookUser = new DefaultOAuth2User(
                List.of(new SimpleGrantedAuthority("ROLE_USER")),
                Map.of("id", "fb-user-1", "email", " Player@Example.COM "),
                "id");

        UserPrincipal principal = service.resolve(facebookUser);

        assertThat(principal.getUserId()).isEqualTo("user-1");
        assertThat(principal.getEmail()).isEqualTo("player@example.com");
        assertThat(principal.getAuthorities())
                .extracting("authority")
                .containsExactly("ROLE_USER");
        verify(socialUserService).resolveOrCreate(" Player@Example.COM ", SocialProvider.FACEBOOK);
    }

    @Test
    void mapsSuspendedAccountToOAuth2Error() {
        when(socialUserService.resolveOrCreate("player@example.com", SocialProvider.FACEBOOK))
                .thenThrow(new UserReportProblems.AccountSuspendedProblem("user-1"));

        DefaultOAuth2User facebookUser = new DefaultOAuth2User(
                List.of(new SimpleGrantedAuthority("ROLE_USER")),
                Map.of("id", "fb-user-1", "email", "player@example.com"),
                "id");

        assertThatThrownBy(() -> service.resolve(facebookUser))
                .isInstanceOfSatisfying(OAuth2AuthenticationException.class, exception ->
                        assertThat(exception.getError().getErrorCode()).isEqualTo("account_suspended"));
    }
}
