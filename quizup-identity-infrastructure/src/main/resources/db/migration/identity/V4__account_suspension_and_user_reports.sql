-- V4: statut de compte (suspension) et signalements de joueurs.

ALTER TABLE user_entry
    ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    ADD COLUMN suspension_id VARCHAR(255),
    ADD COLUMN suspension_reason VARCHAR(40),
    ADD COLUMN suspension_note VARCHAR(500),
    ADD COLUMN suspended_by VARCHAR(255),
    ADD COLUMN suspended_at TIMESTAMP,
    ADD COLUMN suspended_until TIMESTAMP,
    ADD COLUMN reinstated_at TIMESTAMP,
    ADD COLUMN reinstated_by VARCHAR(255),
    ADD COLUMN reinstatement_reason VARCHAR(20),
    ADD COLUMN reinstatement_note VARCHAR(500);

ALTER TABLE user_entry
    ADD CONSTRAINT chk_user_entry_status CHECK (status IN ('ACTIVE', 'SUSPENDED'));

CREATE INDEX idx_user_entry_status_until ON user_entry(status, suspended_until);

COMMENT ON COLUMN user_entry.status IS 'Statut du compte : ACTIVE ou SUSPENDED';
COMMENT ON COLUMN user_entry.suspension_note IS 'Note interne de modération (jamais exposée au joueur)';
COMMENT ON COLUMN user_entry.suspended_until IS 'Échéance de la suspension ; NULL = suspension définitive';

CREATE TABLE user_report (
    report_id VARCHAR(255) PRIMARY KEY,
    thread_id VARCHAR(255) NOT NULL,
    target_user_id VARCHAR(255) NOT NULL,
    reporter_id VARCHAR(255) NOT NULL,
    comment VARCHAR(500),
    context VARCHAR(20) NOT NULL,
    game_id VARCHAR(255),
    target_pseudonym VARCHAR(100),
    target_avatar_options TEXT,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    reviewed_by VARCHAR(255),
    reviewed_at TIMESTAMP,
    resolution_note VARCHAR(500),
    CONSTRAINT fk_user_report_target
        FOREIGN KEY (target_user_id) REFERENCES user_entry(user_id) ON DELETE CASCADE,
    CONSTRAINT fk_user_report_reporter
        FOREIGN KEY (reporter_id) REFERENCES user_entry(user_id) ON DELETE CASCADE,
    CONSTRAINT chk_user_report_status CHECK (status IN ('OPEN', 'RESOLVED', 'DISMISSED')),
    CONSTRAINT chk_user_report_context CHECK (context IN ('PROFILE', 'GAME'))
);

CREATE INDEX idx_user_report_target_status ON user_report(target_user_id, status);
CREATE INDEX idx_user_report_status_created ON user_report(status, created_at);
CREATE INDEX idx_user_report_thread ON user_report(thread_id);

-- Un seul signalement ouvert par couple (joueur signalé × signalant).
CREATE UNIQUE INDEX uq_user_report_open_thread
    ON user_report(thread_id) WHERE status = 'OPEN';

CREATE TABLE user_report_reason (
    report_id VARCHAR(255) NOT NULL,
    reason VARCHAR(32) NOT NULL,
    PRIMARY KEY (report_id, reason),
    CONSTRAINT fk_user_report_reason_report
        FOREIGN KEY (report_id) REFERENCES user_report(report_id) ON DELETE CASCADE,
    CONSTRAINT chk_user_report_reason CHECK (reason IN (
        'HARASSMENT', 'HATE_SPEECH', 'INAPPROPRIATE_CONTENT', 'CHEATING', 'SPAM', 'OTHER'))
);

COMMENT ON TABLE user_report IS 'Signalements de joueurs (file de modération admin)';
COMMENT ON TABLE user_report_reason IS 'Motifs d''un signalement de joueur';
