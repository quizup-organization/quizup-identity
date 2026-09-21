-- V1: Création de la table user_entry et des tables associées
--
-- Cette migration crée la structure complète pour la gestion des utilisateurs.
-- Aucun mot de passe : l'authentification est passwordless (code OTP email) ou sociale.

-- Table principale user_entry
CREATE TABLE IF NOT EXISTS user_entry (
    user_id VARCHAR(255) PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    created_at TIMESTAMP NOT NULL
);

-- Index pour recherche par email
CREATE INDEX idx_user_entry_email ON user_entry(email);

-- Table pour stocker les providers sociaux liés (@ElementCollection)
CREATE TABLE IF NOT EXISTS user_social_providers_entry (
    user_id VARCHAR(255) NOT NULL,
    provider VARCHAR(50) NOT NULL,
    CONSTRAINT fk_user_social_providers_entry_user
        FOREIGN KEY (user_id) REFERENCES user_entry(user_id) ON DELETE CASCADE,
    CONSTRAINT pk_user_social_providers_entry PRIMARY KEY (user_id, provider)
);

-- Index pour recherche par user_id
CREATE INDEX idx_user_social_providers_entry_user_id ON user_social_providers_entry(user_id);

COMMENT ON TABLE user_entry IS 'Table des utilisateurs - projection read-only mise à jour via Event Handlers';
COMMENT ON COLUMN user_entry.user_id IS 'Identifiant unique de l''utilisateur (UUID)';
COMMENT ON COLUMN user_entry.email IS 'Email unique de l''utilisateur';
COMMENT ON COLUMN user_entry.created_at IS 'Date de création du compte';

COMMENT ON TABLE user_social_providers_entry IS 'Providers sociaux liés au compte utilisateur (Google, Facebook, etc.)';
COMMENT ON COLUMN user_social_providers_entry.user_id IS 'Référence vers user_entry';
COMMENT ON COLUMN user_social_providers_entry.provider IS 'Provider social (GOOGLE, FACEBOOK, TWITTER, GITHUB, LINKEDIN)';

-- Codes de connexion à usage unique (OTP email).
-- Le code en clair n'est jamais stocké : seul son hash (BCrypt) est persisté.
CREATE TABLE IF NOT EXISTS user_login_code (
    email VARCHAR(255) PRIMARY KEY,
    code_hash VARCHAR(255) NOT NULL,
    attempts INT NOT NULL DEFAULT 0,
    expires_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL,
    consumed_at TIMESTAMP
);

COMMENT ON TABLE user_login_code IS 'Codes de connexion à usage unique (OTP email) - un seul actif par email';
COMMENT ON COLUMN user_login_code.email IS 'Email destinataire (non normalisé côté SQL, normalisé côté applicatif)';
COMMENT ON COLUMN user_login_code.code_hash IS 'Hash BCrypt du code à 6 chiffres';
COMMENT ON COLUMN user_login_code.attempts IS 'Nombre de tentatives de vérification échouées';
COMMENT ON COLUMN user_login_code.expires_at IS 'Date d''expiration du code';
COMMENT ON COLUMN user_login_code.consumed_at IS 'Date de consommation (usage unique)';

-- Réservation atomique d'email (write-side) : empêche deux inscriptions concurrentes
-- pour le même email (email normalisé en minuscules). Indépendant de la projection user_entry
-- (qui reste la vue de lecture).
CREATE TABLE IF NOT EXISTS email_claim (
    email      VARCHAR(255) PRIMARY KEY,
    user_id    VARCHAR(255) NOT NULL,
    created_at TIMESTAMP    NOT NULL
);

COMMENT ON TABLE email_claim IS 'Réservation unique d''un email lors de l''inscription (anti-doublon concurrent)';
COMMENT ON COLUMN email_claim.email IS 'Email normalisé (minuscules)';
COMMENT ON COLUMN email_claim.user_id IS 'Identifiant de l''utilisateur ayant réservé l''email';
