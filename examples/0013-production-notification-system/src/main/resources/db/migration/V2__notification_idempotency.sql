ALTER TABLE notification_event
    ADD COLUMN idempotency_key VARCHAR(200);

CREATE UNIQUE INDEX uq_notification_tenant_idempotency
    ON notification_event (tenant_id, idempotency_key)
    WHERE idempotency_key IS NOT NULL;
