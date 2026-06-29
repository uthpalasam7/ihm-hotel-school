UPDATE batch_lecturers
SET
    status = 'INACTIVE',
    assignment_end_date = COALESCE(assignment_end_date, CURRENT_DATE),
    updated_at = CURRENT_TIMESTAMP
WHERE id IN (
    SELECT id
    FROM (
        SELECT
            id,
            ROW_NUMBER() OVER (
                PARTITION BY batch_id, lecturer_user_id
                ORDER BY assignment_start_date ASC, created_at ASC, id ASC
            ) AS duplicate_rank
        FROM batch_lecturers
        WHERE status = 'ACTIVE'
    ) duplicate_active_assignments
    WHERE duplicate_rank > 1
);
