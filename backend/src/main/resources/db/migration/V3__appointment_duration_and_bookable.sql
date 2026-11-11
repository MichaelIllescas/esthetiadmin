-- Appointment duration per category (30 or 60 minutes) and online booking flag per service (ADR 013, ADR 014)
ALTER TABLE category
    ADD COLUMN appointment_minutes INT NOT NULL DEFAULT 60,
    ADD CONSTRAINT ck_category_appointment_minutes CHECK (appointment_minutes IN (30, 60));

ALTER TABLE service
    ADD COLUMN bookable BOOLEAN NOT NULL DEFAULT TRUE;

-- Waxing appointments last half an hour; does nothing where the category does not exist
UPDATE category SET appointment_minutes = 30 WHERE name = 'Depilación';
