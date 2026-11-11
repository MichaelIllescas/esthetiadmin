-- Opening hours per weekday and date exceptions for the appointment schedule (ADR 013)
CREATE TABLE availability_rule (
    id          BIGINT  NOT NULL AUTO_INCREMENT,
    day_of_week TINYINT NOT NULL,
    start_time  TIME    NOT NULL,
    end_time    TIME    NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT ck_availability_rule_day CHECK (day_of_week BETWEEN 1 AND 7),
    CONSTRAINT ck_availability_rule_range CHECK (start_time < end_time),
    INDEX idx_availability_rule_day (day_of_week)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

-- Without hours it blocks the whole day; with them, only that range
CREATE TABLE availability_block (
    id         BIGINT       NOT NULL AUTO_INCREMENT,
    block_date DATE         NOT NULL,
    start_time TIME         NULL,
    end_time   TIME         NULL,
    reason     VARCHAR(150) NULL,
    PRIMARY KEY (id),
    CONSTRAINT ck_availability_block_hours CHECK (
        (start_time IS NULL AND end_time IS NULL) OR (start_time IS NOT NULL AND end_time IS NOT NULL AND start_time < end_time)),
    INDEX idx_availability_block_date (block_date)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;
