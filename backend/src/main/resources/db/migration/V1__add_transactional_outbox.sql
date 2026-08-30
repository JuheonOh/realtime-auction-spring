CREATE TABLE outbox_event (
    event_id VARCHAR(36) NOT NULL,
    channel VARCHAR(16) NOT NULL,
    recipient_user_id BIGINT NULL,
    auction_id BIGINT NULL,
    event_type VARCHAR(100) NULL,
    status INT NULL,
    payload LONGTEXT NOT NULL,
    attempts INT NOT NULL DEFAULT 0,
    next_attempt_at DATETIME(6) NOT NULL,
    delivered_at DATETIME(6) NULL,
    last_error VARCHAR(1000) NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (event_id),
    INDEX idx_outbox_event_due (delivered_at, next_attempt_at),
    INDEX idx_outbox_event_auction (auction_id)
);

ALTER TABLE `transaction`
    ADD CONSTRAINT uk_transaction_auction UNIQUE (auction_id);
