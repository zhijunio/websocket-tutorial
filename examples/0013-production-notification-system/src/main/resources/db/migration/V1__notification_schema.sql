CREATE SEQUENCE notification_sequence START WITH 1 INCREMENT BY 1;

CREATE TABLE notification_event (
    id UUID PRIMARY KEY,
    tenant_id VARCHAR(100) NOT NULL,
    event_type VARCHAR(100) NOT NULL,
    recipient_type VARCHAR(30) NOT NULL,
    recipient_id VARCHAR(200) NOT NULL,
    payload JSONB NOT NULL,
    sequence BIGINT NOT NULL DEFAULT nextval('notification_sequence'),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (tenant_id, sequence)
);

CREATE INDEX idx_notification_recipient_sequence
    ON notification_event (tenant_id, recipient_id, sequence);

CREATE TABLE notification_outbox (
    id UUID PRIMARY KEY,
    event_id UUID NOT NULL REFERENCES notification_event(id),
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    attempts INTEGER NOT NULL DEFAULT 0,
    next_attempt_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    locked_until TIMESTAMPTZ,
    last_error VARCHAR(1000),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    published_at TIMESTAMPTZ,
    UNIQUE (event_id)
);

CREATE INDEX idx_outbox_ready
    ON notification_outbox (status, next_attempt_at);

CREATE TABLE notification_delivery (
    event_id UUID NOT NULL REFERENCES notification_event(id),
    tenant_id VARCHAR(100) NOT NULL,
    recipient_id VARCHAR(200) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    delivered_at TIMESTAMPTZ,
    acked_at TIMESTAMPTZ,
    last_error VARCHAR(1000),
    PRIMARY KEY (event_id, recipient_id)
);

CREATE INDEX idx_delivery_pending
    ON notification_delivery (tenant_id, recipient_id, status, event_id);
