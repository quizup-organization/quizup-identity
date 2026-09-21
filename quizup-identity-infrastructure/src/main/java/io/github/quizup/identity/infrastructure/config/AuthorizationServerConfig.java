package io.github.quizup.identity.infrastructure.config;

import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import io.github.quizup.identity.infrastructure.properties.AppProperties;
import io.github.quizup.identity.infrastructure.security.OAuth2UserServiceImpl;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.server.authorization.config.annotation.web.configuration.OAuth2AuthorizationServerConfiguration;
import org.springframework.security.oauth2.server.authorization.config.annotation.web.configurers.OAuth2AuthorizationServerConfigurer;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.util.matcher.AnyRequestMatcher;
import org.springframework.security.web.util.matcher.MediaTypeRequestMatcher;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * Serveur d'autorisation OAuth2/OpenID Connect sans interface hébergée.
 * <p>
 * Toute l'UX est pilotée par la SPA/mobile : le mot de passe est établi par
 * l'API JSON ({@code /api/auth/**}) qui crée une session temporaire
 * (Spring Session JDBC, cookie {@code AUTH_TX}), puis le pipeline standard
 * Authorization Code + PKCE produit les tokens.
 */
@Configuration
@EnableWebSecurity
@EnableConfigurationProperties(AppProperties.class)
public class AuthorizationServerConfig {

    private final OidcUserService oAuth2UserService;
    private final String loginPageUri;
    private final String oauth2SuccessRedirectUri;
    private final String oauth2FailureRedirectUri;
    private final List<String> corsAllowedOrigins;

    public AuthorizationServerConfig(
            OAuth2UserServiceImpl oAuth2UserService,
            AppProperties properties) {
        this.oAuth2UserService = oAuth2UserService;
        this.loginPageUri = properties.security().loginPageUri();
        this.oauth2SuccessRedirectUri = properties.security().oauth2().successRedirectUri();
        this.oauth2FailureRedirectUri = properties.security().oauth2().failureRedirectUri();
        this.corsAllowedOrigins = properties.security().cors().allowedOrigins();
    }

    @Bean
    @Order(1)
    public SecurityFilterChain authorizationServerSecurityFilterChain(
            HttpSecurity http,
            CorsConfigurationSource corsConfigurationSource) throws Exception {

        OAuth2AuthorizationServerConfigurer authorizationServerConfigurer =
                OAuth2AuthorizationServerConfigurer.authorizationServer();

        http
                .securityMatcher(authorizationServerConfigurer.getEndpointsMatcher())
                .with(authorizationServerConfigurer, authorizationServer ->
                        authorizationServer.oidc(Customizer.withDefaults()))
                .csrf(csrf -> csrf.ignoringRequestMatchers(authorizationServerConfigurer.getEndpointsMatcher()))
                .cors(cors -> cors.configurationSource(corsConfigurationSource))
                .exceptionHandling(exceptions -> exceptions
                        .defaultAuthenticationEntryPointFor(
                                new LoginUrlAuthenticationEntryPoint(loginPageUri),
                                new MediaTypeRequestMatcher(MediaType.TEXT_HTML))
                        .defaultAuthenticationEntryPointFor(
                                new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED),
                                AnyRequestMatcher.INSTANCE))
                .authorizeHttpRequests(authorize -> authorize.anyRequest().authenticated());

        return http.build();
    }

    @Bean
    @Order(2)
    public SecurityFilterChain authenticationApiSecurityFilterChain(
            HttpSecurity http,
            CorsConfigurationSource corsConfigurationSource,
            SecurityContextRepository securityContextRepository) throws Exception {

        http
                .securityMatcher("/api/auth/**")
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource))
                .securityContext(securityContext -> securityContext
                        .securityContextRepository(securityContextRepository))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
                .authorizeHttpRequests(authorize -> authorize.anyRequest().permitAll());

        return http.build();
    }

    @Bean
    @Order(3)
    public SecurityFilterChain disabledServerLoginPageSecurityFilterChain(HttpSecurity http) throws Exception {
        // Aucune page de login n'est hébergée par le serveur : l'URL /login est
        // neutralisée pour supprimer la page générée par oauth2Login. Toute l'UX
        // est portée par la SPA (redirection via l'entry point + API JSON).
        http
                .securityMatcher("/login")
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(authorize -> authorize.anyRequest().denyAll());

        return http.build();
    }

    @Bean
    @Order(4)
    public SecurityFilterChain defaultSecurityFilterChain(
            HttpSecurity http,
            CorsConfigurationSource corsConfigurationSource) throws Exception {

        http
                .csrf(csrf -> csrf.ignoringRequestMatchers(
                        "/spring-query-bus-connector/**",
                        "/query-capabilities",
                        "/command-capabilities",
                        "/command-transport",
                        "/spring-command-bus-connector/**"))
                .cors(cors -> cors.configurationSource(corsConfigurationSource))
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(
                                "/error",
                                "/actuator/**",
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/v3/api-docs/**",
                                "/.well-known/**",
                                "/favicon.ico",
                                "/favicon.svg",
                                "/query-capabilities",
                                "/command-capabilities",
                                "/command-transport",
                                "/spring-query-bus-connector/**",
                                "/spring-command-bus-connector/**",
                                "/oauth2/**",
                                "/login/oauth2/**")
                        .permitAll()
                        .anyRequest().authenticated())
                .oauth2Login(oauth2 -> oauth2
                        .userInfoEndpoint(userInfo -> userInfo.oidcUserService(oAuth2UserService))
                        .successHandler(new SimpleUrlAuthenticationSuccessHandler(oauth2SuccessRedirectUri))
                        .failureHandler(new SimpleUrlAuthenticationFailureHandler(oauth2FailureRedirectUri)));

        return http.build();
    }

    @Bean
    public SecurityContextRepository securityContextRepository() {
        return new HttpSessionSecurityContextRepository();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }

    @Bean
    public JwtDecoder jwtDecoder(JWKSource<SecurityContext> jwkSource) {
        return OAuth2AuthorizationServerConfiguration.jwtDecoder(jwkSource);
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowCredentials(true);
        corsAllowedOrigins.forEach(configuration::addAllowedOrigin);
        configuration.addAllowedHeader("*");
        configuration.addAllowedMethod("*");
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
