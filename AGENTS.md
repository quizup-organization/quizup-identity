# AGENTS.md — quizup-identity

> Service **référence** des patterns hexagonaux QuizUp (patterns purs : pas de sous-agrégat, pas
> de saga). Architecture : Axon Framework (CQRS/EDA) + JPA (projections) + Spring Security
> OAuth2 (Authorization Server / IdP).
> Pour les règles de patterns : [
`../../best-practices/.backend/folder-structure.md`](../../best-practices/.backend/folder-structure.md).

---

## 1. Rôle

**IdP** de la plateforme : serveur d'autorisation OIDC (**Authorization Code + PKCE**,
`oidc-react`/`oidc-client-ts` côté client) et gestion de l'identité **immuable**
(email, providers sociaux liés). **Aucun mot de passe** : l'authentification est
**passwordless** (code à usage unique OTP envoyé par email) ou **sociale** (Google).
**Aucun pseudonyme** ici : le profil modifiable (`pseudonym`, `bio`, `country`)
vit dans `quizup-profile` (créé par saga sur `UserRegisteredEvent`).

**Package** : `io.github.quizup.identity`

**Sans UI hébergée** : aucune page HTML/Thymeleaf. Toute l'UX est pilotée par la SPA web
(login OTP + OIDC navigateur) et par le mobile (écran natif OTP + Google). La surface
applicative d'auth est **préfixée par client** (`/api/auth/web/**`, `/api/auth/mobile/**`) ;
les endpoints du protocole OIDC (`/oauth2/**`) restent standard et non préfixés.

### Pipeline d'authentification

```text
Web
  POST /api/auth/web/request-login-code   {email} -> 202
  email contenant un code à 6 chiffres (Resend)
  POST /api/auth/web/login-with-code      {email, code} -> 200 {userId,email} + cookie AUTH_TX
  GET  /oauth2/authorize                  (session trouvée -> authorization code)
  POST /oauth2/token + code_verifier      -> access_token + id_token

Mobile
  POST /api/auth/mobile/request-login-code {email} -> 202
  POST /api/auth/mobile/login-with-code    {email, code} -> session (access + refresh tokens)
  ou POST /api/auth/mobile/login-with-google {idToken} -> session (access + refresh tokens)
  POST /oauth2/token (grant_type=refresh_token) -> rotation
  POST /api/auth/mobile/logout             {refreshToken} -> 204
```

Le premier login **crée le compte** (plus d'étape `/register`). L'API web ne retourne jamais de
JWT ; les endpoints mobiles retournent directement les tokens OAuth2 (mêmes JWT/claims que le
flux web, sans ID token).

Les sessions mobiles sont persistées dans `oauth2_authorization` avec le grant
`urn:quizup:native` ; le refresh standard `/oauth2/token` les fait tourner et le logout les
révoque. Le client public `mobile` conserve aussi ses redirect URIs (`quizup://callback`) pour
les anciens APK qui utilisent encore le flux navigateur : leur login reste fonctionnel via la
page web mise à jour, mais leur logout appelle l'ancien endpoint supprimé (la session locale
est purgée, le refresh token n'est plus révoqué côté identity).

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

### API d'authentification par client

> **Exception auth** : les endpoints sont volontairement **orientés action** pour que le cas
> soit lisible à la lecture (`request-login-code`, `login-with-code`, `login-with-google`,
> `start-google-login`, `logout`). Les autres ressources du service restent orientées
> ressources (`GET /{id}`, transitions `POST /{id}/{action}`).

**Web** (`WebAuthController`) — session cookie + OIDC navigateur :

- `POST /api/auth/web/request-login-code` `{email}` -> `202` (anti-énumération)
- `POST /api/auth/web/login-with-code` `{email, code}` -> `200 {userId,email}` + cookie `AUTH_TX`
- `GET /api/auth/web/start-google-login` -> `302 /oauth2/authorization/google`
- `POST /api/auth/web/logout` `{refreshToken?}` -> `204`

**Mobile** (`MobileAuthController`) — tokens natifs, sans cookie :

- `POST /api/auth/mobile/request-login-code` `{email}` -> `202`
- `POST /api/auth/mobile/login-with-code` `{email, code}` -> `200 {userId,email,accessToken,refreshToken,tokenType,expiresIn}`
- `POST /api/auth/mobile/login-with-google` `{idToken}` -> même réponse
- `POST /api/auth/mobile/logout` `{refreshToken}` -> `204`

Erreurs : `ProblemDetail` (`type`, `title`, `detail`, `status`) ; `401` code/Google invalide,
`403` compte suspendu (`suspendedUntil`), `503` Google indisponible.

### Serveur d'autorisation OIDC (Spring Security standard)

- `/oauth2/authorize`, `/oauth2/token`, `/oauth2/jwks`, `/oauth2/revoke`, `/oauth2/introspect`
- `/.well-known/openid-configuration`, `/connect/logout`
- Social : `/oauth2/authorization/google` -> `/login/oauth2/code/google`
  (succès -> redirection SPA `app.security.oauth2.success-redirect-uri`).

En cas de non-authentification sur `/oauth2/authorize` : `302` vers la SPA
(`app.security.login-page-uri`) pour HTML, sinon `401` JSON. **Jamais de page `/login` serveur.**
L'URL d'autorisation d'origine est transmise en `?continue=` (`SpaLoginRedirectEntryPoint`) :
la SPA la reprend après login pour rejoindre le client tiers (Grafana, ArgoCD, anciens APK
mobiles…).

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
- `RecordUserLoginUseCase` — enregistrement d'une connexion réussie (OTP ou sociale, 1re connexion incluse)
- `GetUserUseCase`, `FindUserUseCase`, `CheckUserUseCase`, `SearchUserUseCase`

**Queries** (`domain/query/UserQuery.java`) : `UserSearchQuery`, `UserExistsByIdQuery`,
`UserExistsByEmailQuery`, `GetUserQuery`, `FindUserQuery`, `FindUserByEmailQuery`,
`GetUsersByIdsQuery`, `GetUserAccountStatusQuery`, `GetUserAccountStatusesQuery`.

### Suspension de compte et signalements de joueurs

- **Statut de compte** porté par `UserAggregate` : `AccountStatus` (`ACTIVE|SUSPENDED`),
  `UserSuspension` (motif, note interne, acteur, `suspendedUntil` `null` = définitive,
  réintégration). Commandes `SuspendUserCommand` / `ReinstateUserCommand` ; événements
  `UserSuspendedEvent` / `UserReinstatedEvent` (email inclus, autosuffisants).
- **Garde-fous** : compte système (`SYSTEM_USER_ID`) jamais suspendable ; double suspension
  refusée ; réintégration idempotente ; une suspension temporaire échue est **effectivement
  active** même avant le passage du balayeur (`AccountStatusRules`).
- **Expiration** : `SuspensionExpirySweeper` (`@Scheduled`) envoie une réintégration système
  `EXPIRED` pour les échéances dépassées. La lecture du statut ne bloque jamais un compte dont
  l'échéance est passée.
- **Application** : OTP (`verifyCode`), login social, générateur de tokens (`invalid_grant`
  `account_suspended` pour code/refresh), suppression des sessions `AUTH_TX` et autorisations
  OAuth2 à la suspension. Un email est envoyé au joueur (la note reste interne).
- **Signalements** : `UserReportThreadAggregate` (thread déterministe joueur signalé × signalant,
  un seul `OPEN`), snapshot pseudonyme/avatar + contexte `PROFILE|GAME`. Queries
  `UserReportQuery.*` (détail, liste par cible, file groupée, stats).
- **Migration** `V4__account_suspension_and_user_reports.sql` (`user_entry` + `user_report` +
  `user_report_reason`, index unique partiel `(thread_id) WHERE status='OPEN'`).
- **Pas d'auto-suspension** : la décision est exclusivement admin.

---

## 4. Dépendances inter-services

**Aucune dépendance sortante.** Identity est un **fournisseur** sur le bus partagé :

- Événement `UserEvent.UserRegisteredEvent(userId, email, provider, createdAt)` ->
  consommé par `quizup-profile` (saga `CreateProfileSaga`).
- Événement `UserEvent.SocialProviderLinkedEvent(userId, provider, linkedAt)` (mise à jour locale).
- Événement `UserEvent.UserLoggedInEvent(userId, loggedInAt)` — émis à chaque connexion réussie
  (OTP ou sociale, y compris la première), exploité par les métriques Axon (`quizup_axon_events_published_total`).
- Événement `UserEvent.UserSuspendedEvent(userId, email, suspensionId, reason, note, suspendedBy,
  suspendedAt, suspendedUntil)` — consommé par `quizup-notification` (inbox obligatoire) et le BFF
  (invalidation de cache + fermeture des accès).
- Événement `UserEvent.UserReinstatedEvent(userId, email, suspensionId, reason, note, reinstatedBy,
  reinstatedAt)` — consommé par le BFF.
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
| `quizup.account-suspension.sweep-interval-ms` | Fréquence du balayeur des suspensions temporaires échues (défaut 60 000 ms) |
| `app.mail.api-key` / `app.mail.from` / `app.mail.base-url` | Envoi des codes OTP via Resend (`QUIZUP_MAIL_API_KEY`, …) |
| `app.auth.dev-fixed-code` | Code OTP fixe réservé au profil `local`/E2E |
| `app.auth.mobile.client-id` | Client OAuth2 public utilisé par les endpoints natifs (`mobile`) |
| `app.auth.mobile.google-client-id` | Client OAuth2 **Web** Google : audience des ID tokens du SDK natif |
| `app.jwk.jwk-set` | JWK Set JSON partagée (secret `QUIZUP_IDENTITY_JWK`) |
| `app.security.login-page-uri` | SPA login (entry point non authentifié) |
| `app.security.oauth2.success-redirect-uri` / `failure-redirect-uri` | Retour social |
| `app.security.cors.allowed-origins` | Origine SPA (credentials) |
| `app.security.session.cookie-*` | Cookie `AUTH_TX` (name/same-site/secure) |
| `spring.security.oauth2.authorizationserver.client.{web,mobile}.token` | Tokens des clients SPA/mobile : access `PT1H`, refresh **`P365D`** (sessions longues), rotation `reuse-refresh-tokens: false` — clients construits en mémoire au démarrage (rollout image suffisant) |
