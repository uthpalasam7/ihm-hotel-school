ALTER TABLE class_sessions ADD COLUMN rescheduling_reason VARCHAR(2000);
ALTER TABLE class_sessions ADD CONSTRAINT ck_class_sessions_rescheduling_reason
    CHECK (status <> 'RESCHEDULED' OR
        (rescheduling_reason IS NOT NULL AND CHAR_LENGTH(TRIM(rescheduling_reason)) > 0));
-- Each original has one immediate replacement; further moves form a chain.
ALTER TABLE class_sessions ADD CONSTRAINT uk_class_sessions_replacement UNIQUE (original_session_id);
ALTER TABLE class_sessions ADD CONSTRAINT ck_class_sessions_replacement_origin
    CHECK (original_session_id IS NULL OR (source_schedule_id IS NULL AND generation_date IS NULL));
