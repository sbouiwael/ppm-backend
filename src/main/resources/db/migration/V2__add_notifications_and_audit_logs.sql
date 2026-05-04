-- ==============================================================
-- PPM BIAT — V2: Add notifications and audit_logs tables
-- These tables were created by Hibernate ddl-auto=update in dev
-- and must be explicitly declared here for production (ddl-auto=validate + Flyway).
-- All text columns use utf8mb4 to support full Unicode (emojis, accents, special chars).
-- ==============================================================

-- ── Notifications ─────────────────────────────────────────────
-- Entity: Notification.java
-- Stores real-time and persisted user notifications (task assignments, status changes, etc.)
CREATE TABLE IF NOT EXISTS notifications (
    id           BIGINT          NOT NULL AUTO_INCREMENT,
    recipient_id BIGINT          NOT NULL,
    type         VARCHAR(40)     NOT NULL,
    title        VARCHAR(200)    NOT NULL,
    message      VARCHAR(500),
    is_read      BOOLEAN         NOT NULL DEFAULT FALSE,
    entity_type  VARCHAR(50),
    entity_id    BIGINT,
    created_at   DATETIME        NOT NULL,
    PRIMARY KEY (id),
    INDEX idx_notif_recipient_read (recipient_id, is_read),
    INDEX idx_notif_created        (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ── Audit Logs ────────────────────────────────────────────────
-- Entity: AuditLog.java
-- Denormalised audit trail — actor email/role and entity name are copied at write time
-- so history remains readable even after renames or soft-deletes.
CREATE TABLE IF NOT EXISTS audit_logs (
    id           BIGINT          NOT NULL AUTO_INCREMENT,
    actor_id     BIGINT,
    actor_email  VARCHAR(200)    NOT NULL,
    actor_role   VARCHAR(50),
    action       VARCHAR(30)     NOT NULL,
    entity_type  VARCHAR(50)     NOT NULL,
    entity_id    BIGINT          NOT NULL,
    entity_name  VARCHAR(255),
    details      VARCHAR(2000),
    project_id   BIGINT,
    project_name VARCHAR(255),
    timestamp    DATETIME        NOT NULL,
    PRIMARY KEY (id),
    INDEX idx_audit_entity  (entity_type, entity_id),
    INDEX idx_audit_actor   (actor_id),
    INDEX idx_audit_ts      (timestamp),
    INDEX idx_audit_project (project_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
