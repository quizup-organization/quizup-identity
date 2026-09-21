package io.github.quizup.identity.infrastructure.config;

import io.github.quizup.identity.infrastructure.properties.AppProperties;
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
    public CookieSerializer cookieSerializer(AppProperties properties) {
        AppProperties.Security.Session session = properties.security().session();

        DefaultCookieSerializer serializer = new DefaultCookieSerializer();
        serializer.setCookieName(session.cookieName());
        serializer.setSameSite(session.cookieSameSite());
        serializer.setUseSecureCookie(session.cookieSecure());
        serializer.setCookiePath(session.cookiePath());
        serializer.setUseHttpOnlyCookie(true);
        return serializer;
    }
}
