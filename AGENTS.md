# AGENTS.md — quizup-identity

> Service **référence** des patterns hexagonaux QuizUp (patterns purs : pas de sous-agrégat, pas
> de saga). Architecture : Axon Framework (CQRS/EDA) + JPA (projections) + Spring Security
> OAuth2 (Authorization Server / IdP).
> Pour les règles de patterns : [
`../../best-practices/.backend/hexagonal-architecture.md`](../../best-practices/.backend/hexagonal-architecture.md).

---

## 1. Rôle

**IdP** de la plateforme : serveur d'autorisation OIDC (**Authorization Code + PKCE**,
`oidc-react`/`oidc-client-ts` côté client) et gestion de l'identité **immuable**
(email, providers sociaux liés). **Aucun mot de passe** : l'authentification est
**passwordless** (code à usage unique OTP envoyé par email) ou **sociale** (Google).
**Aucun nom d'affichage** ici : le profil modifiable (`displayName`, `bio`, `country`)
vit dans `quizup-profile` (créé par saga sur `UserRegisteredEvent`).

**Package** : `io.github.quizup.identity`

**Sans UI hébergée** : aucune page HTML/Thymeleaf. Toute l'UX est pilotée par la SPA/mobile.
La session interactive temporaire est établie par la **vérification du code OTP** (API JSON),
puis le pipeline OIDC standard émet les tokens.

### Pipeline d'authentification

```text
SPA /login
  -> POST /api/auth/request-code    (JSON {email}, credentials: include) -> 202
  -> email contenant un code à 6 chiffres (Resend)
  -> POST /api/auth/verify-code     (JSON {email, code}) -> 200 {userId,email}
  -> session temporaire PostgreSQL (Spring Session JDBC, cookie opaque AUTH_TX)
  -> GET /oauth2/authorize           (session trouvée -> authorization code)
  -> POST /oauth2/token + code_verifier
  -> access_token + id_token
```

Le premier `verify-code` **crée le compte** (plus d'étape `/register`). L'API JSON ne retourne
jamais de JWT.

### Compte système unique

`QuizUpConstants.SYSTEM_USER_ID` / `SYSTEM_USER_EMAIL` (`quizup.contacts@gmail.com`) est un compte
**unique** (admin + bot), **sans credential**, non connectable (exclu du flux OTP). Il est seedé par
`DataSeeder` (`registerUser`, `app.seed-data.enabled`).

### Multi-instance (N instances, sans sticky session)

- **Sessions** : Spring Session JDBC, cookie `AUTH_TX` (host-only) — `SessionConfig`.
- **Codes OTP** : table `user_login_code` (hash BCrypt, TTL, tentatives, usage unique) → partagés.
- **Clients OAuth2 entrants** : déclarés via les propriétés **standard** Spring Boot
  `spring.security.oauth2.authorizationserver.client.*` (`application.yml` + redirect URIs
  par profil), exposés en **in-memory** (`InMemoryRegisteredClientRepository`) par
  `OAuth2ClientConfig` (secret encodé BCrypt, cf. plus bas).
- **Authorization codes, tokens, consentements** : JDBC
  (`JdbcOAuth2AuthorizationService`, `JdbcOAuth2AuthorizationConsentService`) —
  `OAuth2PersistenceConfig` (état dynamique partagé entre instances ; le registre de
  clients, lui, est statique et identique sur chaque pod).
- **Clés de signature JWT partagées** : JWK Set fournie par le secret Kubernetes
  (`QUIZUP_IDENTITY_JWK` -> `app.jwk.jwk-set`) ; fallback éphémère réservé au profil `local`.

---

## 2. Endpoints

### API JSON d'authentification (`AuthController`)

- `POST /api/auth/request-code` `{email}` -> `202` (toujours, anti-énumération ; envoie le code OTP)
- `POST /api/auth/verify-code` `{email, code}` -> `200 {userId,email}` | `401` (code invalide/expiré)
- `POST /api/auth/logout` -> `204` (invalide la session)

### Serveur d'autorisation OIDC (Spring Security standard)

- `/oauth2/authorize`, `/oauth2/token`, `/oauth2/jwks`, `/oauth2/revoke`, `/oauth2/introspect`
- `/.well-known/openid-configuration`, `/connect/logout`
- Social : `/oauth2/authorization/google` -> `/login/oauth2/code/google`
  (succès -> redirection SPA `app.security.oauth2.success-redirect-uri`).

En cas de non-authentification sur `/oauth2/authorize` : `302` vers la SPA
(`app.security.login-page-uri`) pour HTML, sinon `401` JSON. **Jamais de page `/login` serveur.**

### Routes internes

- Connecteurs Axon (`/query-capabilities`, `/command-capabilities`, `/command-transport`, …)
- Actuator, Swagger (`/v3/api-docs`, `/swagger-ui`)

**JWT** : `JwtTokenCustomizer` émet `sub`, `email`, `user_email`, `user_id`, `roles`
et `aud` (si `app.authorization-server.audience` renseigné) — **aucun claim `name`**.
Les rôles viennent de `Roles.forUser(userId, email)` : `ROLE_USER` pour tous, plus `ROLE_ADMIN` pour
le compte système et pour toute adresse de `app.authorization-server.admin-emails`
(consommé en aval, ex. `role_attribute_path` Grafana).

---

## 3. Use cases (ports entrants — `domain/port/in/`)

Exposés **sur le bus Axon**, sans REST (sauf l'API JSON d'auth) :

- `RegisterUserUseCase` — enregistrement sans credential (passwordless/système) ou via provider social
- `PasswordlessAuthUseCase` — demande + vérification du code OTP (implémenté par `PasswordlessAuthService`)
- `LinkSocialProviderUseCase` — liaison d'un provider à un compte existant
- `GetUserUseCase`, `FindUserUseCase`, `CheckUserUseCase`, `SearchUserUseCase`

**Queries** (`domain/query/UserQuery.java`) : `UserSearchQuery`, `UserExistsByIdQuery`,
`UserExistsByEmailQuery`, `GetUserQuery`, `FindUserQuery`, `FindUserByEmailQuery`.

---

## 4. Dépendances inter-services

**Aucune dépendance sortante.** Identity est un **fournisseur** sur le bus partagé :

- Événement `UserEvent.UserRegisteredEvent(userId, email, provider, createdAt)` ->
  consommé par `quizup-profile` (saga `CreateProfileSaga`).
- Événement `UserEvent.SocialProviderLinkedEvent(userId, provider, linkedAt)` (mise à jour locale).
- Les queries `UserQuery.*` restent exposées sur le bus.

**Ports sortants locaux** : `LoginCodeRepositoryPort` (OTP), `EmailSenderPort` (Resend),
`UserRepositoryPort` (persistance).

---

## 5. Configuration clé

| Propriété | Rôle |
|---|---|
| `app.authorization-server.issuer` | Issuer OIDC public (doit matcher l'authority de la SPA) |
| `app.authorization-server.audience` | Claim `aud` (optionnel) |
| `app.authorization-server.admin-emails` | Allowlist d'emails → `ROLE_ADMIN` (env `QUIZUP_ADMIN_EMAILS`) |
| `app.mail.api-key` / `app.mail.from` / `app.mail.base-url` | Envoi des codes OTP via Resend (`QUIZUP_MAIL_API_KEY`, …) |
| `app.auth.dev-fixed-code` | Code OTP fixe réservé au profil `local`/E2E |
| `app.jwk.jwk-set` | JWK Set JSON partagée (secret `QUIZUP_IDENTITY_JWK`) |
| `app.security.login-page-uri` | SPA login (entry point non authentifié) |
| `app.security.oauth2.success-redirect-uri` / `failure-redirect-uri` | Retour social |
| `app.security.cors.allowed-origins` | Origine SPA (credentials) |
| `app.security.session.cookie-*` | Cookie `AUTH_TX` (name/same-site/secure) |
