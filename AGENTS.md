# AGENTS.md — quizup-identity

> Service **référence** des patterns hexagonaux QuizUp (patterns purs : pas de sous-agrégat, pas
> de saga). Architecture : Axon Framework (CQRS/EDA) + JPA (projections) + Spring Security OAuth2.
> Pour les règles de patterns : [
`../../best-practices/hexagonal-architecture.md`](../../best-practices/hexagonal-architecture.md).

---

## 1. Rôle

Authentification (OIDC/Resource Server) et gestion des utilisateurs : enregistrement,
récupération, recherche paginée. **Source de vérité** pour les utilisateurs — appelé par les
autres services, mais ne dépend d'aucun service.

**Package** : `io.github.quizup.identity`

---

## 2. Endpoints REST

### `AuthenticationController` — `/api/authentication`

| Méthode | Chemin                   | Handler | Response          |
|---------|--------------------------|---------|-------------------|
| GET     | `/api/authentication/me` | `me()`  | `QuizUpPrincipal` |

### `UserController` — `/api/users` (`@CrossOrigin`)

| Méthode | Chemin                | Handler                 | Response                     |
|---------|-----------------------|-------------------------|------------------------------|
| POST    | `/api/users/search`   | `search(SearchRequest)` | `PageResponse<UserResponse>` |
| GET     | `/api/users/{userId}` | `getUserById(String)`   | `UserResponse`               |

### Pages web (Thymeleaf, non REST)

- `HomeController` `GET /` → view `home`
- `LoginController` `GET /login` → view `login`

**DTO** : `UserResponse(userId, email, name, Set<SocialProvider> linkedSocialAccounts, Instant createdAt)`

---

## 3. Use cases (ports entrants — `domain/port/in/`)

- `RegisterUserUseCase` — enregistrement (password ou provider)
- `GetUserUseCase` — récupération par id
- `FindUserUseCase` — existence / lookup léger
- `CheckUserUseCase` — vérification d'attributs utilisateur
- `SearchUserUseCase` — recherche paginée

---

## 4. Dépendances inter-services

**Aucune dépendance sortante.** Identity est un **fournisseur** de données : il est appelé par
`quizup-social` (UserPort → `UserQuery.FindUserQuery` / `UserExistsByIdQuery`) et
`quizup-matchmaking` (UserPort → `UserQuery.FindUserQuery`). Aucun `QueryGateway` sortant.

**Ports sortants locaux** : `PasswordEncoderPort` (BCrypt), `UserRepositoryPort` (persistance).

