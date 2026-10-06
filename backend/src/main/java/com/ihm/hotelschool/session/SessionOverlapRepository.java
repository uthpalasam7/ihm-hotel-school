package com.ihm.hotelschool.session;

import java.sql.Types;
import java.time.LocalDate;
import java.time.LocalTime;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class SessionOverlapRepository {
    private final JdbcTemplate jdbc;

    public SessionOverlapRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    public boolean exists(Long batchId, LocalDate date, LocalTime start, LocalTime end, Long excludeId) {
        // Direct JDBC 4.2 binding keeps query times local, matching the entity's LOCAL_TIME mapping.
        // Hibernate's inferred JPQL parameter type otherwise applies the UTC timestamp setting to TIME.
        // JdbcTemplate participates in the same JPA transaction and batch-row lock.
        return Boolean.TRUE.equals(jdbc.query("""
                select exists (select 1 from class_sessions
                where batch_id = ? and session_date = ? and status in ('SCHEDULED', 'COMPLETED')
                and start_time < ? and end_time > ? and id <> ?)
                """, statement -> {
            statement.setLong(1, batchId);
            statement.setObject(2, date, Types.DATE);
            statement.setObject(3, end, Types.TIME);
            statement.setObject(4, start, Types.TIME);
            statement.setLong(5, excludeId == null ? 0L : excludeId);
        }, result -> result.next() && result.getBoolean(1)));
    }
}
