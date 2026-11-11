-- Appointments and the 30-minute slots they occupy (ADR 013)
CREATE TABLE appointment (
    id         BIGINT       NOT NULL AUTO_INCREMENT,
    service_id BIGINT       NOT NULL,
    client_id  BIGINT       NULL,
    starts_at  DATETIME     NOT NULL,
    ends_at    DATETIME     NOT NULL,
    first_name VARCHAR(100) NOT NULL,
    last_name  VARCHAR(100) NOT NULL,
    phone      VARCHAR(15)  NOT NULL,
    status     VARCHAR(12)  NOT NULL,
    source     VARCHAR(6)   NOT NULL,
    notes      TEXT         NULL,
    created_at DATETIME     NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_appointment_service FOREIGN KEY (service_id) REFERENCES service (id),
    CONSTRAINT fk_appointment_client FOREIGN KEY (client_id) REFERENCES client (id),
    CONSTRAINT ck_appointment_range CHECK (starts_at < ends_at),
    CONSTRAINT ck_appointment_status CHECK (status IN ('CONFIRMED', 'CANCELLED', 'ATTENDED', 'NO_SHOW')),
    CONSTRAINT ck_appointment_source CHECK (source IN ('PUBLIC', 'ADMIN')),
    INDEX idx_appointment_starts_at (starts_at),
    INDEX idx_appointment_phone (phone),
    INDEX idx_appointment_client (client_id),
    INDEX idx_appointment_service (service_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

-- One row per 30-minute slot an active appointment takes: the unique key makes overlaps impossible
CREATE TABLE appointment_slot (
    id             BIGINT   NOT NULL AUTO_INCREMENT,
    appointment_id BIGINT   NOT NULL,
    slot_start     DATETIME NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_appointment_slot_start UNIQUE (slot_start),
    CONSTRAINT fk_appointment_slot_appointment FOREIGN KEY (appointment_id) REFERENCES appointment (id)
        ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;
