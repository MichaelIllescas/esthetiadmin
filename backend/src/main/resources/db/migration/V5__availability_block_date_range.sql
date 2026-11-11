-- A block covers a range of days (vacations are one block, not one per day); hours only on a single day (ADR 013)
ALTER TABLE availability_block
    ADD COLUMN start_date DATE NULL AFTER id,
    ADD COLUMN end_date   DATE NULL AFTER start_date;

UPDATE availability_block SET start_date = block_date, end_date = block_date;

ALTER TABLE availability_block
    MODIFY start_date DATE NOT NULL,
    MODIFY end_date   DATE NOT NULL,
    DROP CHECK ck_availability_block_hours,
    DROP INDEX idx_availability_block_date,
    DROP COLUMN block_date;

ALTER TABLE availability_block
    ADD CONSTRAINT ck_availability_block_dates CHECK (start_date <= end_date),
    ADD CONSTRAINT ck_availability_block_hours CHECK (
        (start_time IS NULL AND end_time IS NULL)
        OR (start_time IS NOT NULL AND end_time IS NOT NULL AND start_time < end_time AND start_date = end_date)),
    ADD INDEX idx_availability_block_dates (end_date, start_date);
