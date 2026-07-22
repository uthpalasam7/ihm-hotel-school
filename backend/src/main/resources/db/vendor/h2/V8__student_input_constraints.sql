UPDATE students
SET nic = UPPER(REGEXP_REPLACE(TRIM(nic), '\s+', '')),
    normalized_nic = UPPER(REGEXP_REPLACE(TRIM(normalized_nic), '\s+', '')),
    contact_number = TRIM(contact_number),
    alternative_contact_number = NULLIF(TRIM(alternative_contact_number), ''),
    gender = NULLIF(TRIM(gender), '');

ALTER TABLE students
    ADD CONSTRAINT ck_students_nic_alphanumeric CHECK (REGEXP_LIKE(nic, '^[A-Z0-9]+$'));

ALTER TABLE students
    ADD CONSTRAINT ck_students_normalized_nic_alphanumeric CHECK (REGEXP_LIKE(normalized_nic, '^[A-Z0-9]+$'));

ALTER TABLE students
    ADD CONSTRAINT ck_students_contact_number_digits CHECK (REGEXP_LIKE(contact_number, '^[0-9]{10}$'));

ALTER TABLE students
    ADD CONSTRAINT ck_students_alternative_contact_number_digits CHECK (alternative_contact_number IS NULL OR REGEXP_LIKE(alternative_contact_number, '^[0-9]{10}$'));

ALTER TABLE students
    ADD CONSTRAINT ck_students_gender_values CHECK (gender IS NULL OR gender IN ('Male', 'Female', 'Other'));
