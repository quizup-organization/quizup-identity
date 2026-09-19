# Construction d'un serveur d'autorisation Spring multi-instance
## SPA et mobile, authentification sociale ou username/password, PostgreSQL et PKCE

## Préambule : ce que l'on veut construire

L'objectif est de construire un **serveur d'autorisation OAuth 2.0 / OpenID Connect centralisé avec Spring Authorization Server**, sans aucune page de connexion, ressource HTML ou vue Thymeleaf hébergée par le serveur. Toute l'expérience utilisateur est pilotée par une SPA React ou par une application mobile : affichage du formulaire username/password, choix d'un fournisseur social, gestion des erreurs et navigation applicative.

Le serveur doit proposer plusieurs méthodes d'authentification, notamment :

- username/password saisi dans la SPA ou l'application mobile ;
- Google, Microsoft, GitHub ou un autre fournisseur social fédéré ;
- à terme, MFA, passkey ou une autre méthode forte.

Quelle que soit la méthode choisie, le résultat doit être **strictement homogène pour le client**. Une connexion locale par mot de passe et une connexion sociale doivent toutes les deux converger vers un flux standard **Authorization Code + PKCE**. La SPA ou le mobile reçoit toujours un authorization code interne, l'échange toujours sur `/oauth2/token` avec son `code_verifier`, puis obtient les mêmes types de tokens, le même issuer, la même audience et le même modèle de claims.

Une bibliothèque OIDC standard telle que `oidc-react`, fondée sur `oidc-client-ts`, doit pouvoir rester responsable de la mécanique protocolaire commune : découverte OIDC, génération de `state` / `nonce` et PKCE, gestion du callback, échange du code et exposition de l'utilisateur authentifié. Le code ne prend pas en charge l'interface d'authentication et, pour le mot de passe, l'établissement préalable d'une session d'authentification temporaire via l'API JSON du serveur.

Le serveur doit pouvoir être déployé en **N instances interchangeables**, sans sticky session et sans état conservé dans la mémoire locale d'un nœud. Les sessions interactives temporaires sont matérialisées par un cookie opaque `AUTH_TX` et persistées dans PostgreSQL avec Spring Session JDBC. Les clients OAuth, authorization codes, consentements, identités locales et liaisons sociales sont également persistés dans PostgreSQL. Les clés de signature sont partagées entre toutes les instances.

Le résultat recherché est donc le pipeline unique suivant :

```text
SPA ou mobile
    -> choix de la méthode d'authentification
    -> authentification locale ou fédérée
    -> session temporaire partagée dans PostgreSQL
    -> /oauth2/authorize
    -> authorization code interne
    -> /oauth2/token + code_verifier
    -> access_token + id_token
```

Le endpoint username/password ne constitue pas un Password Grant et ne retourne jamais directement un JWT. Il authentifie la transaction navigateur, puis le pipeline standard Authorization Code + PKCE produit les tokens. Ainsi, les applications et les resource servers n'ont pas besoin de distinguer une connexion locale d'une connexion sociale.