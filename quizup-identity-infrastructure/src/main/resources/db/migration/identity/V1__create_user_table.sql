-- V1: Création de la table user_entry et des tables associées
--
-- Cette migration crée la structure complète pour la gestion des utilisateurs

-- Table principale user_entry
CREATE TABLE IF NOT EXISTS user_entry (
    user_id VARCHAR(255) PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255),
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

-- Commentaires pour documentation
COMMENT ON TABLE user_entry IS 'Table des utilisateurs - projection read-only mise à jour via Event Handlers';
COMMENT ON COLUMN user_entry.user_id IS 'Identifiant unique de l''utilisateur (UUID)';
COMMENT ON COLUMN user_entry.email IS 'Email unique de l''utilisateur';
COMMENT ON COLUMN user_entry.password IS 'Hash du mot de passe (null pour OAuth2/social)';
COMMENT ON COLUMN user_entry.created_at IS 'Date de création du compte';

COMMENT ON TABLE user_social_providers_entry IS 'Providers sociaux liés au compte utilisateur (Google, Facebook, etc.)';
COMMENT ON COLUMN user_social_providers_entry.user_id IS 'Référence vers user_entry';
COMMENT ON COLUMN user_social_providers_entry.provider IS 'Provider social (GOOGLE, FACEBOOK, TWITTER, GITHUB, LINKEDIN)';
