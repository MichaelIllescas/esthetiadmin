-- Optional email of the client and of the appointment, used only for appointment notices (ADR 016)
ALTER TABLE client
    ADD COLUMN email VARCHAR(254) NULL AFTER phone;

ALTER TABLE appointment
    ADD COLUMN email VARCHAR(254) NULL AFTER phone;

-- The business settings: a single row, created empty
CREATE TABLE business_settings (
    id                 BIGINT       NOT NULL,
    notification_email VARCHAR(254) NULL,
    contact_phone      VARCHAR(15)  NULL,
    PRIMARY KEY (id),
    CONSTRAINT ck_business_settings_single_row CHECK (id = 1)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

INSERT INTO business_settings (id, notification_email, contact_phone) VALUES (1, NULL, NULL);
