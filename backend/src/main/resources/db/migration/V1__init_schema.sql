CREATE TABLE category (
    id     BIGINT       NOT NULL AUTO_INCREMENT,
    name   VARCHAR(100) NOT NULL,
    active BOOLEAN      NOT NULL DEFAULT TRUE,
    PRIMARY KEY (id),
    CONSTRAINT uk_category_name UNIQUE (name)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE service (
    id          BIGINT         NOT NULL AUTO_INCREMENT,
    category_id BIGINT         NOT NULL,
    name        VARCHAR(150)   NOT NULL,
    description TEXT           NULL,
    price       DECIMAL(10, 2) NOT NULL,
    active      BOOLEAN        NOT NULL DEFAULT TRUE,
    PRIMARY KEY (id),
    CONSTRAINT uk_service_category_name UNIQUE (category_id, name),
    CONSTRAINT fk_service_category FOREIGN KEY (category_id) REFERENCES category (id),
    CONSTRAINT ck_service_price CHECK (price >= 0)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE client (
    id         BIGINT       NOT NULL AUTO_INCREMENT,
    first_name VARCHAR(100) NOT NULL,
    last_name  VARCHAR(100) NOT NULL,
    phone      VARCHAR(30)  NULL,
    birth_date DATE         NULL,
    notes      TEXT         NULL,
    PRIMARY KEY (id),
    INDEX idx_client_name (last_name, first_name)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE treatment (
    id           BIGINT         NOT NULL AUTO_INCREMENT,
    client_id    BIGINT         NOT NULL,
    service_id   BIGINT         NOT NULL,
    performed_on DATE           NOT NULL,
    amount       DECIMAL(10, 2) NOT NULL,
    notes        TEXT           NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_treatment_client FOREIGN KEY (client_id) REFERENCES client (id),
    CONSTRAINT fk_treatment_service FOREIGN KEY (service_id) REFERENCES service (id),
    CONSTRAINT ck_treatment_amount CHECK (amount >= 0),
    INDEX idx_treatment_client_date (client_id, performed_on),
    INDEX idx_treatment_service (service_id),
    INDEX idx_treatment_date (performed_on)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE expense_type (
    id   BIGINT       NOT NULL AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_expense_type_name UNIQUE (name)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE expense (
    id              BIGINT         NOT NULL AUTO_INCREMENT,
    expense_type_id BIGINT         NOT NULL,
    spent_on        DATE           NOT NULL,
    description     VARCHAR(255)   NOT NULL,
    amount          DECIMAL(10, 2) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_expense_type FOREIGN KEY (expense_type_id) REFERENCES expense_type (id),
    CONSTRAINT ck_expense_amount CHECK (amount > 0),
    INDEX idx_expense_type (expense_type_id),
    INDEX idx_expense_date (spent_on)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE app_user (
    id            BIGINT       NOT NULL AUTO_INCREMENT,
    username      VARCHAR(50)  NOT NULL,
    password_hash VARCHAR(100) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_app_user_username UNIQUE (username)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;
