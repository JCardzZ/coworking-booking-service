-- Existing rows get 'system'; new rows are filled by JPA auditing.
ALTER TABLE spaces
    ADD COLUMN created_by VARCHAR(150) NOT NULL DEFAULT 'system',
    ADD COLUMN updated_by VARCHAR(150) NOT NULL DEFAULT 'system';

ALTER TABLE spaces
    ALTER COLUMN created_by DROP DEFAULT,
    ALTER COLUMN updated_by DROP DEFAULT;
