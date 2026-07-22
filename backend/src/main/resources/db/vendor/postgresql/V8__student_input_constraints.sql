UPDATE students
SET nic = upper(regexp_replace(trim(nic), '\s+', '', 'g')),
    normalized_nic = upper(regexp_replace(trim(normalized_nic), '\s+', '', 'g')),
    contact_number = trim(contact_number),
    alternative_contact_number = nullif(trim(alternative_contact_number), ''),
    gender = nullif(trim(gender), '');

ALTER TABLE students
    ADD CONSTRAINT ck_students_nic_alphanumeric CHECK (nic ~ '^[A-Z0-9]+$'),
    ADD CONSTRAINT ck_students_normalized_nic_alphanumeric CHECK (normalized_nic ~ '^[A-Z0-9]+$'),
    ADD CONSTRAINT ck_students_contact_number_digits CHECK (contact_number ~ '^[0-9]{10}$'),
    ADD CONSTRAINT ck_students_alternative_contact_number_digits CHECK (alternative_contact_number IS NULL OR alternative_contact_number ~ '^[0-9]{10}$'),
    ADD CONSTRAINT ck_students_gender_values CHECK (gender IS NULL OR gender IN ('Male', 'Female', 'Other'));
