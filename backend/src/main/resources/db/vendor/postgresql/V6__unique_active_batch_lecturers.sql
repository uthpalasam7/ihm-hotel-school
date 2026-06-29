CREATE UNIQUE INDEX uk_batch_lecturers_active_batch_lecturer
    ON batch_lecturers (batch_id, lecturer_user_id)
    WHERE status = 'ACTIVE';
