package io.github.quizup.identity.infrastructure.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.session.jdbc.config.annotation.web.http.EnableJdbcHttpSession;
import org.springframework.session.web.http.CookieSerializer;
import org.springframework.session.web.http.DefaultCookieSerializer;

/**
 * Sessions HTTP persistées en JDBC (Spring Session) et matérialisées par le
 * cookie opaque {@code AUTH_TX}. Sans état local, elles sont partagées par
 * toutes les instances derrière le load balancer (pas de sticky session).
 */
@Configuration
@EnableJdbcHttpSession(maxInactiveIntervalInSeconds = 1800)
public class SessionConfig {

    @Bean
    public CookieSerializer cookieSerializer(
            @Value("${app.security.session.cookie-name:AUTH_TX}") String cookieName,
            @Value("${app.security.session.cookie-same-site:Lax}") String sameSite,
            @Value("${app.security.session.cookie-secure:false}") boolean secure,
            @Value("${app.security.session.cookie-path:/}") String path) {

        DefaultCookieSerializer serializer = new DefaultCookieSerializer();
        serializer.setCookieName(cookieName);
        serializer.setSameSite(sameSite);
        serializer.setUseSecureCookie(secure);
        serializer.setCookiePath(path);
        serializer.setUseHttpOnlyCookie(true);
        return serializer;
    }
}
