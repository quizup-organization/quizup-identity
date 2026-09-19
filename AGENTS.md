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
(email, hash de mot de passe, providers sociaux liés). **Aucun nom d'affichage** ici :
le profil modifiable (`displayName`, `bio`, `country`) vit dans `quizup-profile` (créé
par saga sur `UserRegisteredEvent`).

**Package** : `io.github.quizup.identity`

**Sans UI hébergée** : aucune page HTML/Thymeleaf. Toute l'UX est pilotée par la SPA/mobile.
Le mot de passe est établi par une **API JSON** qui crée une session interactive temporaire,
puis le pipeline OIDC standard émet les tokens.

### Pipeline d'authentification

```text
SPA /login
  -> POST /api/auth/login|register   (JSON, credentials: include)
  -> session temporaire PostgreSQL (Spring Session JDBC, cookie opaque AUTH_TX)
  -> GET /oauth2/authorize           (session trouvée -> authorization code)
  -> POST /oauth2/token + code_verifier
  -> access_token + id_token
```

Le endpoint username/password **n'est pas un Password Grant** et ne retourne jamais de JWT.

### Multi-instance (N instances, sans sticky session)

- **Sessions** : Spring Session JDBC, cookie `AUTH_TX` (host-only) — `SessionConfig`.
- **Clients OAuth2, authorization codes, tokens, consentements** : JDBC
  (`JdbcRegisteredClientRepository`, `JdbcOAuth2AuthorizationService`,
  `JdbcOAuth2AuthorizationConsentService`) — `OAuth2PersistenceConfig`.
- **Clients seedés** depuis `authentication.oauth2.clients` (`application.yml`), id déterministe = clientId.
- **Clés de signature JWT partagées** : JWK Set fournie par le secret Kubernetes
  (`QUIZUP_IDENTITY_JWK` -> `app.jwk.jwk-set`) ; fallback éphémère réservé au profil `local`.

---

## 2. Endpoints

### API JSON d'authentification (`AuthController`)

- `POST /api/auth/login` `{email,password}` -> `200 {userId,email}` | `401`
- `POST /api/auth/register` `{email,password}` -> `201 {userId,email}` | `400/409`
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

---

## 3. Use cases (ports entrants — `domain/port/in/`)

Exposés **sur le bus Axon**, sans REST (sauf l'API JSON d'auth) :

- `RegisterUserUseCase` — enregistrement (password ou provider)
- `LinkSocialProviderUseCase` — liaison d'un provider à un compte existant
- `GetUserUseCase`, `FindUserUseCase`, `CheckUserUseCase`, `SearchUserUseCase`

**Queries** (`domain/query/UserQuery.java`) : `UserSearchQuery`, `UserExistsByIdQuery`,
`UserExistsByEmailQuery`, `GetUserQuery`, `FindUserQuery`, `FindUserByEmailQuery`.

---

## 4. Dépendances inter-services

**Aucune dépendance sortante.** Identity est un **fournisseur** sur le bus partagé :

- Événement `UserEvent.UserRegisteredEvent(userId, email, password, provider, createdAt)` ->
  consommé par `quizup-profile` (saga `CreateProfileSaga`).
- Événement `UserEvent.SocialProviderLinkedEvent(userId, provider, linkedAt)` (mise à jour locale).
- Les queries `UserQuery.*` restent exposées sur le bus.

**Ports sortants locaux** : `PasswordEncoderPort` (BCrypt), `UserRepositoryPort` (persistance).

---

## 5. Configuration clé

| Propriété | Rôle |
|---|---|
| `app.authorization-server.issuer` | Issuer OIDC public (doit matcher l'authority de la SPA) |
| `app.authorization-server.audience` | Claim `aud` (optionnel) |
| `app.jwk.jwk-set` | JWK Set JSON partagée (secret `QUIZUP_IDENTITY_JWK`) |
| `app.security.login-page-uri` | SPA login (entry point non authentifié) |
| `app.security.oauth2.success-redirect-uri` / `failure-redirect-uri` | Retour social |
| `app.security.cors.allowed-origins` | Origine SPA (credentials) |
| `app.security.session.cookie-*` | Cookie `AUTH_TX` (name/same-site/secure) |
