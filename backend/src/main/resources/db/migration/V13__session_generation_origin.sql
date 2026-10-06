ALTER TABLE batch_schedules ADD CONSTRAINT uk_batch_schedules_batch_id UNIQUE (batch_id, id);
ALTER TABLE class_sessions ADD COLUMN source_schedule_id BIGINT;
ALTER TABLE class_sessions ADD COLUMN generation_date DATE;
ALTER TABLE class_sessions ADD CONSTRAINT fk_sessions_source_schedule
    FOREIGN KEY (batch_id, source_schedule_id) REFERENCES batch_schedules(batch_id, id);
ALTER TABLE class_sessions ADD CONSTRAINT ck_sessions_generation_origin CHECK (
    (source_schedule_id IS NULL AND generation_date IS NULL)
    OR (source_schedule_id IS NOT NULL AND generation_date IS NOT NULL));
-- Retain the original generation slot even after cancellation/rescheduling.
ALTER TABLE class_sessions ADD CONSTRAINT uk_sessions_generation_origin UNIQUE (source_schedule_id, generation_date);
