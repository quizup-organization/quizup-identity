-- V5: suppression de compte (RGPD) — statut DELETED, codes OTP dédiés et audit.

ALTER TABLE user_entry
    DROP CONSTRAINT IF EXISTS chk_user_entry_status;

ALTER TABLE user_entry
    ADD CONSTRAINT chk_user_entry_status
        CHECK (status IN ('ACTIVE', 'SUSPENDED', 'DELETED'));

-- Un email ne doit être réservé que par un compte encore actif/suspendu. Un ancien compte
-- DELETED garde temporairement son email jusqu'au traitement de l'événement, mais ne doit pas
-- empêcher une réinscription immédiate avec le même email.
ALTER TABLE user_entry DROP CONSTRAINT IF EXISTS user_entry_email_key;
CREATE UNIQUE INDEX IF NOT EXISTS uq_user_entry_active_email
    ON user_entry(email) WHERE status <> 'DELETED';

CREATE TABLE IF NOT EXISTS account_deletion_code (
    email       VARCHAR(255) PRIMARY KEY,
    user_id     VARCHAR(255) NOT NULL,
    code_hash   VARCHAR(255) NOT NULL,
    attempts    INTEGER NOT NULL DEFAULT 0,
    expires_at  TIMESTAMP NOT NULL,
    created_at  TIMESTAMP NOT NULL,
    consumed_at TIMESTAMP
);

COMMENT ON TABLE account_deletion_code IS 'Codes OTP de confirmation de suppression de compte';

CREATE TABLE IF NOT EXISTS account_deletion_audit (
    deletion_id  VARCHAR(255) PRIMARY KEY,
    user_id      VARCHAR(255) NOT NULL,
    requested_at TIMESTAMP NOT NULL,
    completed_at TIMESTAMP,
    status       VARCHAR(20) NOT NULL,
    CONSTRAINT chk_account_deletion_status
        CHECK (status IN ('REQUESTED', 'COMPLETED', 'FAILED'))
);

CREATE INDEX IF NOT EXISTS idx_account_deletion_user ON account_deletion_audit(user_id);
COMMENT ON TABLE account_deletion_audit IS 'Audit de suppression sans donnée personnelle';
