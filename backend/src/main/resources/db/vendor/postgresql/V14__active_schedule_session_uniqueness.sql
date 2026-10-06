CREATE UNIQUE INDEX uk_batch_schedules_active_slot
    ON batch_schedules(batch_id, day_of_week, start_time) WHERE status = 'ACTIVE';
CREATE UNIQUE INDEX uk_class_sessions_active_slot
    ON class_sessions(batch_id, session_date, start_time, (COALESCE(lecturer_user_id, 0)))
    WHERE status IN ('SCHEDULED', 'COMPLETED');
