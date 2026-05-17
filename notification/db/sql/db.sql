-- =========================================
-- NOTIFICATION SERVICE SCHEMA
-- Schema itself is created by accounts/db/sql/db.sql (which declares all schemas).
-- Run this file after that one against the same database.
-- =========================================

SET search_path = notification_service, public;

CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- =========================================
-- NOTIFICATIONS
-- One row per notification request. id doubles as the idempotency key.
-- =========================================
CREATE TABLE notifications (
    id UUID PRIMARY KEY,

    channel VARCHAR(20) NOT NULL,
    recipient VARCHAR(320) NOT NULL,
    template_key VARCHAR(100) NOT NULL,
    template_variables JSONB,

    status VARCHAR(20) NOT NULL,
    provider_used VARCHAR(50),
    provider_message_id VARCHAR(255),

    attempts INTEGER NOT NULL DEFAULT 0,
    last_error TEXT,

    user_id BIGINT,

    sent_at TIMESTAMPTZ,
    delivered_at TIMESTAMPTZ,
    failed_at TIMESTAMPTZ,

    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT ck_notifications_channel
        CHECK (channel IN ('EMAIL','SMS','WHATSAPP','PUSH')),
    CONSTRAINT ck_notifications_status
        CHECK (status IN ('PENDING','SENT','DELIVERED','FAILED','BOUNCED','SUPPRESSED'))
);

CREATE INDEX idx_notifications_retry
    ON notifications(status, updated_at)
    WHERE status IN ('PENDING','FAILED');

CREATE INDEX idx_notifications_provider_msg
    ON notifications(provider_used, provider_message_id)
    WHERE provider_message_id IS NOT NULL;

CREATE INDEX idx_notifications_user_id
    ON notifications(user_id) WHERE user_id IS NOT NULL;

CREATE INDEX idx_notifications_recipient
    ON notifications(recipient);

CREATE TRIGGER trg_notifications_updated_at
    BEFORE UPDATE ON notifications
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

-- =========================================
-- SUPPRESSION LIST
-- Recipients we must never send to (hard bounces, complaints, unsubs).
-- =========================================
CREATE TABLE suppression_list (
    id BIGSERIAL PRIMARY KEY,

    channel VARCHAR(20) NOT NULL,
    recipient VARCHAR(320) NOT NULL,
    reason VARCHAR(30) NOT NULL,
    source_provider VARCHAR(50),
    notes TEXT,

    added_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uq_suppression_channel_recipient UNIQUE (channel, recipient),
    CONSTRAINT ck_suppression_channel
        CHECK (channel IN ('EMAIL','SMS','WHATSAPP','PUSH')),
    CONSTRAINT ck_suppression_reason
        CHECK (reason IN ('HARD_BOUNCE','COMPLAINT','UNSUBSCRIBE','INVALID','MANUAL'))
);

CREATE INDEX idx_suppression_recipient ON suppression_list(recipient);

-- =========================================
-- DELIVERY EVENTS
-- Raw webhook payloads from providers; processed asynchronously.
-- =========================================
CREATE TABLE delivery_events (
    id BIGSERIAL PRIMARY KEY,

    provider VARCHAR(50) NOT NULL,
    event_type VARCHAR(50) NOT NULL,
    provider_message_id VARCHAR(255),
    notification_id UUID,

    raw_payload JSONB NOT NULL,

    received_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    processed_at TIMESTAMPTZ,
    processing_error TEXT
);

CREATE INDEX idx_delivery_events_unprocessed
    ON delivery_events(received_at)
    WHERE processed_at IS NULL;

CREATE INDEX idx_delivery_events_provider_msg
    ON delivery_events(provider, provider_message_id)
    WHERE provider_message_id IS NOT NULL;
