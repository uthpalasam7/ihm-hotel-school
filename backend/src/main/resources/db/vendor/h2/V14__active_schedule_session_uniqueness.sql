-- Generated keys mirror PostgreSQL partial indexes, including a null lecturer.
ALTER TABLE batch_schedules ADD active_batch_key BIGINT GENERATED ALWAYS AS
    (CASE WHEN status = 'ACTIVE' THEN batch_id ELSE NULL END);
CREATE UNIQUE INDEX uk_batch_schedules_active_slot ON batch_schedules(active_batch_key, day_of_week, start_time);
ALTER TABLE class_sessions ADD active_batch_key BIGINT GENERATED ALWAYS AS
    (CASE WHEN status IN ('SCHEDULED', 'COMPLETED') THEN batch_id ELSE NULL END);
ALTER TABLE class_sessions ADD lecturer_slot_key BIGINT GENERATED ALWAYS AS (COALESCE(lecturer_user_id, 0));
CREATE UNIQUE INDEX uk_class_sessions_active_slot ON class_sessions(active_batch_key, session_date, start_time, lecturer_slot_key);
