## [2.7.0](https://github.com/quizup-organization/quizup-identity/compare/v2.6.0...v2.7.0) (2026-10-18)

### Features

* **identity:** add account moderation and native mobile authentication ([65beb9e](https://github.com/quizup-organization/quizup-identity/commit/65beb9e1efb489bdf11b9d7ede564bb579efddb5))

## [2.6.0](https://github.com/quizup-organization/quizup-identity/compare/v2.5.0...v2.6.0) (2026-10-12)

### Features

* **console:** consume quizup-sdk 4.7.0 (lag, commandes en vol, snapshots, scheduled) ([2bfffa5](https://github.com/quizup-organization/quizup-identity/commit/2bfffa5fb5ffcf22d02ac23b0e7c804881ea877d))

## [2.5.0](https://github.com/quizup-organization/quizup-identity/compare/v2.4.0...v2.5.0) (2026-10-12)

### Features

* **console:** consume quizup-sdk 4.6.0 (build-info + API interne event store/scheduled) ([4888d2b](https://github.com/quizup-organization/quizup-identity/commit/4888d2b27abafb54faf2f382a96bf3a0264d57c8))

## [2.4.0](https://github.com/quizup-organization/quizup-identity/compare/v2.3.1...v2.4.0) (2026-10-12)

### Features

* **console:** consume quizup-sdk 4.5.2 (DLQ Axon par défaut + agent console) ([3b0a681](https://github.com/quizup-organization/quizup-identity/commit/3b0a681dcdc37090f0fff869e2761c581f8738ce))

## [2.3.1](https://github.com/quizup-organization/quizup-identity/compare/v2.3.0...v2.3.1) (2026-10-12)

### Bug Fixes

* **oidc:** redirects console.quizup.local pour le client console (parallèle aux autres clients) ([8d55adc](https://github.com/quizup-organization/quizup-identity/commit/8d55adca46f38d11868a1e5704aed994f7d1ed57))

## [2.3.0](https://github.com/quizup-organization/quizup-identity/compare/v2.2.1...v2.3.0) (2026-10-12)

### Features

* **oidc:** client public console (PKCE) pour la console d'exploitation ([db54608](https://github.com/quizup-organization/quizup-identity/commit/db54608973a44426e1179f1c20e283f597e2bf4c))

## [2.2.1](https://github.com/quizup-organization/quizup-identity/compare/v2.2.0...v2.2.1) (2026-10-11)

### Bug Fixes

* **auth:** preserve third-party authorization request through SPA login ([b64d0f0](https://github.com/quizup-organization/quizup-identity/commit/b64d0f015cbdafbdf0924511d238146575a04c48))

## [2.2.0](https://github.com/quizup-organization/quizup-identity/compare/v2.1.1...v2.2.0) (2026-10-11)

### Features

* **auth:** add OIDC client for ArgoCD ([a37df61](https://github.com/quizup-organization/quizup-identity/commit/a37df61f4c4d6dce10b62dbc1627028d3b8ab116))

## [2.1.1](https://github.com/quizup-organization/quizup-identity/compare/v2.1.0...v2.1.1) (2026-10-11)

### Bug Fixes

* **identity:** sessions web/mobile 365 jours (refresh token) ([b01b787](https://github.com/quizup-organization/quizup-identity/commit/b01b787135aaf103cf218ff18df3efb3472a2bd8))

## [2.1.0](https://github.com/quizup-organization/quizup-identity/compare/v2.0.0...v2.1.0) (2026-10-11)

### Features

* **auth:** emit UserLoggedInEvent on every successful login ([5c1cd79](https://github.com/quizup-organization/quizup-identity/commit/5c1cd7921f8dfb8b2073594a044380c3913e0e39))

## [2.0.0](https://github.com/quizup-organization/quizup-identity/compare/v1.8.3...v2.0.0) (2026-09-25)

### ⚠ BREAKING CHANGES

* **identity:** the user search use case and repository ports now use the
SDK core SearchRequest/SearchResponse DTOs; the previous custom pagination
model is removed.

### Features

* **identity:** migrate to SDK SearchRequest/SearchResponse contracts ([3667676](https://github.com/quizup-organization/quizup-identity/commit/366767661ca62661d6473170650e60570bc1fc5e))

## [1.8.3](https://github.com/quizup-organization/quizup-identity/compare/v1.8.2...v1.8.3) (2026-09-24)

### Bug Fixes

* **deps:** bump quizup-parent to 2.4.4 (typed PageResult over query transport) ([fa62722](https://github.com/quizup-organization/quizup-identity/commit/fa627226c65b0383b2c8cccdd256e8a7ab318cf4))

## [1.8.2](https://github.com/quizup-organization/quizup-identity/compare/v1.8.1...v1.8.2) (2026-09-24)

### Bug Fixes

* **deps:** bump quizup-parent to 2.4.3 (bus-only search criteria type info) ([8e6f4c6](https://github.com/quizup-organization/quizup-identity/commit/8e6f4c62ade6c624f9d928c0154402e0f812174b))

## [1.8.1](https://github.com/quizup-organization/quizup-identity/compare/v1.8.0...v1.8.1) (2026-09-24)

### Bug Fixes

* **deps:** bump quizup-parent to 2.4.2 (search criteria type info) ([d81db46](https://github.com/quizup-organization/quizup-identity/commit/d81db46a4541be51e4c32ebd624d11a03ea4f37e))

## [1.8.0](https://github.com/quizup-organization/quizup-identity/compare/v1.7.4...v1.8.0) (2026-09-23)

### Features

* **identity:** passwordless auth as resources (login-codes/sessions) and bump SDK to 2.4.1 ([0ee48aa](https://github.com/quizup-organization/quizup-identity/commit/0ee48aa24d9d9323b85576844a2dec7885499fd0))

## [1.7.4](https://github.com/quizup-organization/quizup-identity/compare/v1.7.3...v1.7.4) (2026-09-22)

### Bug Fixes

* **quizup-identity:** upgrade quizup-parent to 2.3.4 ([2b0c65a](https://github.com/quizup-organization/quizup-identity/commit/2b0c65a5c6906a02dadc703a0a2f803364cd05d8))

## [1.7.3](https://github.com/quizup-organization/quizup-identity/compare/v1.7.2...v1.7.3) (2026-09-22)

### Bug Fixes

* **quizup-identity:** upgrade quizup-parent to 2.3.3 ([9729991](https://github.com/quizup-organization/quizup-identity/commit/9729991ce1a6fad3d0a439a05bdc07f83fa438c6))

## [1.7.2](https://github.com/quizup-organization/quizup-identity/compare/v1.7.1...v1.7.2) (2026-09-22)

### Bug Fixes

* **quizup-identity:** upgrade quizup-parent to 2.3.2 ([cee392b](https://github.com/quizup-organization/quizup-identity/commit/cee392b9a74d63b3c02822696f769c3fc8e8e5ac))

## [1.7.1](https://github.com/quizup-organization/quizup-identity/compare/v1.7.0...v1.7.1) (2026-09-22)

### Bug Fixes

* **identity:** upgrade quizup-parent to 2.3.1 (registration address fix) ([164750e](https://github.com/quizup-organization/quizup-identity/commit/164750ebe28e6f4354e8714a7a2f574758aabbab))

## [1.7.0](https://github.com/quizup-organization/quizup-identity/compare/v1.6.0...v1.7.0) (2026-09-22)

### Features

* **identity:** public-client refresh tokens, explicit Axon processing groups and default seeding ([4a5dec9](https://github.com/quizup-organization/quizup-identity/commit/4a5dec9aa17621707e59e8e47dd50ad33de36281))

## [1.6.0](https://github.com/quizup-organization/quizup-identity/compare/v1.5.3...v1.6.0) (2026-09-21)

### Features

* **identity:** atomic email reservation against concurrent sign-ups ([9b096b6](https://github.com/quizup-organization/quizup-identity/commit/9b096b66f223536a81019073e99f672684c45866))
* **identity:** extend token lifetimes for web, mobile and grafana ([92ab472](https://github.com/quizup-organization/quizup-identity/commit/92ab472b790bb6b62cb0e2f26a30150c43788750))

### Bug Fixes

* **observability:** keep readable console logs in local ([8b1de91](https://github.com/quizup-organization/quizup-identity/commit/8b1de916f578e73b1c604a833373780b57aa7eaa))

## [1.5.3](https://github.com/quizup-organization/quizup-identity/compare/v1.5.2...v1.5.3) (2026-09-20)

### Bug Fixes

* **deps:** bump quizup-sdk to 2.1.1 ([4216592](https://github.com/quizup-organization/quizup-identity/commit/42165925f794ba268d5fa5e718267345e7f07f01))

## [1.5.2](https://github.com/quizup-organization/quizup-identity/compare/v1.5.1...v1.5.2) (2026-09-20)

### Bug Fixes

* **deps:** bump quizup-sdk to 2.1.0 ([faf051b](https://github.com/quizup-organization/quizup-identity/commit/faf051ba4d468fbb277166f78cfdd5eda78b4d29))

## [1.5.1](https://github.com/quizup-organization/quizup-identity/compare/v1.5.0...v1.5.1) (2026-09-20)

### Bug Fixes

* **deps:** bump quizup-sdk to 2.0.0 ([4d89800](https://github.com/quizup-organization/quizup-identity/commit/4d89800c92bdc5915d875749772c27589efc8d13))

## [1.5.0](https://github.com/quizup-organization/quizup-identity/compare/v1.4.3...v1.5.0) (2026-09-20)

### Features

* **auth:** replace password with email OTP and email-based admin role ([9699400](https://github.com/quizup-organization/quizup-identity/commit/9699400e0a3607e4160fe74343686ed0228c27de))

## [1.4.3](https://github.com/quizup-organization/quizup-identity/compare/v1.4.2...v1.4.3) (2026-09-20)

### Bug Fixes

* **observability:** remove WebSocket and business KPI metrics (consume quizup-sdk 1.4.3) ([9e8a7c3](https://github.com/quizup-organization/quizup-identity/commit/9e8a7c3a6943f8bfee62c2cee1f55829e35c8d89))

## [1.4.2](https://github.com/quizup-organization/quizup-identity/compare/v1.4.1...v1.4.2) (2026-09-20)

### Bug Fixes

* **observability:** consume quizup-sdk 1.4.2 (Axon activity metrics fix + Swagger server URL) ([0a40edf](https://github.com/quizup-organization/quizup-identity/commit/0a40edf82b4eaf11b8891487b9308e51f84580ad))

## [1.4.1](https://github.com/quizup-organization/quizup-identity/compare/v1.4.0...v1.4.1) (2026-09-20)

### Bug Fixes

* **swagger:** allow the gateway Swagger UI redirect URIs ([942e9e7](https://github.com/quizup-organization/quizup-identity/commit/942e9e717d36c6bfd551e5c2ed0724401c1617a5))

## [1.4.0](https://github.com/quizup-organization/quizup-identity/compare/v1.3.0...v1.4.0) (2026-09-20)

### Features

* **observability:** consume quizup-sdk 1.4.0 (Axon activity metrics) ([6b9d2c2](https://github.com/quizup-organization/quizup-identity/commit/6b9d2c2c461f888c3526b9a44a182c1398f4450f))

## [1.3.0](https://github.com/quizup-organization/quizup-identity/compare/v1.2.0...v1.3.0) (2026-09-20)

### Features

* **observability:** consume quizup-sdk 1.3.0 (WebSocket metrics) ([a8bdd1e](https://github.com/quizup-organization/quizup-identity/commit/a8bdd1e725635544ce63e924418eb2ac4ed2c9cc))

## [1.2.0](https://github.com/quizup-organization/quizup-identity/compare/v1.1.0...v1.2.0) (2026-09-20)

### Features

* **observability:** emit ROLE_ADMIN and auth KPIs (consume quizup-sdk 1.2.0) ([b111edb](https://github.com/quizup-organization/quizup-identity/commit/b111edbc0f9ee092265a08140b4e0364de16887e))

## [1.1.0](https://github.com/quizup-organization/quizup-identity/compare/v1.0.1...v1.1.0) (2026-09-20)

### Features

* **observability:** add Grafana OIDC client and consume quizup-sdk 1.1.0 ([806aff6](https://github.com/quizup-organization/quizup-identity/commit/806aff6a5e588d4dd9ac3d7d511d0d8efca4aa3a))

## [1.0.1](https://github.com/quizup-organization/quizup-identity/compare/v1.0.0...v1.0.1) (2026-09-19)

### Bug Fixes

* **oidc:** allow the production web redirect URI (app.quizup.cnadjim.fr/callback) ([25ec42b](https://github.com/quizup-organization/quizup-identity/commit/25ec42b66a762be4b663d4184efda945aaf165d9))

## 1.0.0 (2026-09-19)

### Features

* first commit ([f162735](https://github.com/quizup-organization/quizup-identity/commit/f162735c7ca8bd1a20463afe1a70994f0f202d84))

### Bug Fixes

* **maven:** pin inter-service domains to 0.0.1; align parent to quizup-parent 1.0.0 ([a8d10d4](https://github.com/quizup-organization/quizup-identity/commit/a8d10d41b8e2c7745dc514ba7b24af456db9b67c))
