package io.github.quizup.identity.infrastructure.config;

import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import io.github.quizup.identity.infrastructure.properties.OAuth2ClientsProperties;
import io.github.quizup.identity.infrastructure.security.OAuth2UserServiceImpl;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.authorization.client.InMemoryRegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.config.annotation.web.configuration.OAuth2AuthorizationServerConfiguration;
import org.springframework.security.oauth2.server.authorization.config.annotation.web.configurers.OAuth2AuthorizationServerConfigurer;
import org.springframework.security.oauth2.server.authorization.settings.AuthorizationServerSettings;
import org.springframework.security.oauth2.server.authorization.settings.ClientSettings;
import org.springframework.security.oauth2.server.authorization.settings.TokenSettings;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.util.matcher.MediaTypeRequestMatcher;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Configuration du serveur d'autorisation OAuth2/OpenID Connect
 */
@Configuration
@EnableWebSecurity
@EnableConfigurationProperties(OAuth2ClientsProperties.class)
public class AuthorizationServerConfig {

    private final OAuth2ClientsProperties clientsProperties;
    private final OidcUserService oAuth2UserService;

    public AuthorizationServerConfig(OAuth2ClientsProperties clientsProperties,
                                     OAuth2UserServiceImpl oAuth2UserService) {
        this.clientsProperties = clientsProperties;
        this.oAuth2UserService = oAuth2UserService;
    }

    @Bean
    @Order(1)
    public SecurityFilterChain authorizationServerSecurityFilterChain(HttpSecurity http,
                                                                      CorsConfigurationSource corsConfigurationSource) throws Exception {

        OAuth2AuthorizationServerConfigurer authorizationServerConfigurer =
                OAuth2AuthorizationServerConfigurer.authorizationServer();

        http
                .securityMatcher(authorizationServerConfigurer.getEndpointsMatcher())


                .with(authorizationServerConfigurer, authorizationServer ->
                        authorizationServer
                                .oidc(Customizer.withDefaults())
                )

                .authorizeHttpRequests(authorize ->
                        authorize.anyRequest().authenticated()
                )

                .exceptionHandling(exceptions -> exceptions
                        .defaultAuthenticationEntryPointFor(
                                new LoginUrlAuthenticationEntryPoint("/login"),
                                new MediaTypeRequestMatcher(MediaType.TEXT_HTML)
                        )
                )

                .oauth2ResourceServer(resourceServer ->
                        resourceServer.jwt(Customizer.withDefaults())
                )
                .cors(cors -> cors.configurationSource(corsConfigurationSource));

        return http.build();
    }

    @Bean
    @Order(2)
    public SecurityFilterChain defaultSecurityFilterChain(HttpSecurity http,
                                                          CorsConfigurationSource corsConfigurationSource) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource))
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(
                                "/login",
                                "/error",
                                "/css/**",
                                "/js/**",
                                "/actuator/**",
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/v3/api-docs/**",
                                "/.well-known/**",
                                "/favicon.ico")
                        .permitAll()
                        .anyRequest().authenticated()
                )
                .formLogin(form -> form
                        .loginPage("/login")
                        .usernameParameter("email")  // Utilise le champ "email" au lieu de "username"
                        .passwordParameter("password")
                        .permitAll()
                )
                .oauth2Login(oauth2 -> oauth2
                        .loginPage("/login")
                        .userInfoEndpoint(userInfo -> userInfo
                                .oidcUserService(oAuth2UserService)  // Pour les providers OIDC comme Google
                        )
                        // Note : pas de defaultSuccessUrl pour ne pas interférer avec le flow OAuth2 Authorization Code
                        .failureUrl("/login?error")
                )
                .logout(logout -> logout
                        .logoutSuccessUrl("/login?logout")
                        .permitAll()
                );

        return http.build();
    }

    @Bean
    public RegisteredClientRepository registeredClientRepository(PasswordEncoder passwordEncoder) {
        List<RegisteredClient> clients = new ArrayList<>();

        if (clientsProperties.getClients() != null) {
            clientsProperties.getClients().forEach((name, config) -> {
                RegisteredClient.Builder builder = RegisteredClient.withId(UUID.randomUUID().toString())
                        .clientId(config.getClientId() != null ? config.getClientId() : name);

                // Client Secret
                if (config.getClientSecret() != null && !config.getClientSecret().isBlank()) {
                    builder.clientSecret(passwordEncoder.encode(config.getClientSecret()));
                }

                // Authentication Methods
                for (String method : config.getAuthenticationMethods()) {
                    builder.clientAuthenticationMethod(parseAuthenticationMethod(method));
                }

                // Grant Types
                for (String grantType : config.getGrantTypes()) {
                    builder.authorizationGrantType(parseGrantType(grantType));
                }

                // Scopes
                for (String scope : config.getScopes()) {
                    builder.scope(scope);
                }

                // Redirect URIs
                for (String uri : config.getRedirectUris()) {
                    builder.redirectUri(uri);
                }

                // Post Logout Redirect URIs
                for (String uri : config.getPostLogoutRedirectUris()) {
                    builder.postLogoutRedirectUri(uri);
                }

                // Client Settings
                builder.clientSettings(ClientSettings.builder()
                        .requireProofKey(config.isRequireProofKey())
                        .requireAuthorizationConsent(config.isRequireAuthorizationConsent())
                        .build());

                // Token Settings
                builder.tokenSettings(TokenSettings.builder()
                        .accessTokenTimeToLive(config.getTokenConfig().getAccessTokenTimeToLive())
                        .refreshTokenTimeToLive(config.getTokenConfig().getRefreshTokenTimeToLive())
                        .reuseRefreshTokens(config.getTokenConfig().isReuseRefreshTokens())
                        .build());

                clients.add(builder.build());
            });
        }

        return new InMemoryRegisteredClientRepository(clients);
    }

    @Bean
    public JwtDecoder jwtDecoder(JWKSource<SecurityContext> jwkSource) {
        return OAuth2AuthorizationServerConfiguration.jwtDecoder(jwkSource);
    }

    @Bean
    public AuthorizationServerSettings authorizationServerSettings() {
        return AuthorizationServerSettings.builder()
                .issuer("http://localhost:8085")
                .build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowCredentials(true);
        config.addAllowedOriginPattern("*");
        config.addAllowedHeader("*");
        config.addAllowedMethod("*");
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    private ClientAuthenticationMethod parseAuthenticationMethod(String method) {
        return switch (method.toLowerCase()) {
            case "client_secret_post" -> ClientAuthenticationMethod.CLIENT_SECRET_POST;
            case "client_secret_jwt" -> ClientAuthenticationMethod.CLIENT_SECRET_JWT;
            case "private_key_jwt" -> ClientAuthenticationMethod.PRIVATE_KEY_JWT;
            case "none" -> ClientAuthenticationMethod.NONE;
            default -> ClientAuthenticationMethod.CLIENT_SECRET_BASIC;
        };
    }

    private AuthorizationGrantType parseGrantType(String grantType) {
        return switch (grantType.toLowerCase()) {
            case "refresh_token" -> AuthorizationGrantType.REFRESH_TOKEN;
            case "client_credentials" -> AuthorizationGrantType.CLIENT_CREDENTIALS;
            default -> AuthorizationGrantType.AUTHORIZATION_CODE;
        };
    }
}
