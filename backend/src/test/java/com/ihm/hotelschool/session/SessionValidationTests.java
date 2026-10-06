package com.ihm.hotelschool.session;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.ihm.hotelschool.batch.CourseBatch;
import java.time.*;
import org.junit.jupiter.api.Test;

class SessionValidationTests {
    private final LocalDate start = LocalDate.of(2026, 7, 1);
    private final LocalDate end = LocalDate.of(2026, 9, 30);
    private final LocalTime morning = LocalTime.of(9, 0);
    private final LocalTime afternoon = LocalTime.of(13, 0);

    @Test void sessionDatesIncludeBothBatchBoundaries() {
        assertThat(session(start).getSessionDate()).isEqualTo(start);
        assertThat(session(end).getSessionDate()).isEqualTo(end);
        assertThatThrownBy(() -> session(start.minusDays(1))).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> session(end.plusDays(1))).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> session(null)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test void sessionsAndPatternsRequireForwardSameDayTimes() {
        for (LocalTime invalid : new LocalTime[]{morning, LocalTime.of(8, 0), null}) {
            assertThatThrownBy(() -> new ClassSession(batch(), start, morning, invalid, null, null, null, null, Instant.now(), 1L))
                    .isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> new BatchSchedule(batch(), 1, morning, invalid, null, null, Instant.now(), 1L))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Test void patternsUseIsoWeekdayOneToSeven() {
        for (int day : new int[]{0, 8}) {
            assertThatThrownBy(() -> new BatchSchedule(batch(), day, morning, afternoon, null, null, Instant.now(), 1L))
                    .isInstanceOf(IllegalArgumentException.class);
        }
        assertThat(new BatchSchedule(batch(), 7, morning, afternoon, null, null, Instant.now(), 1L).getDayOfWeek()).isEqualTo(7);
    }

    @Test void optionalTextIsTrimmedAndLengthChecked() {
        var session = new ClassSession(batch(), start, morning, afternoon, null, " Safety ", "   ", null, Instant.now(), 1L);
        assertThat(session.getTopic()).isEqualTo("Safety");
        assertThat(session.getClassroom()).isNull();
        assertThatThrownBy(() -> new ClassSession(batch(), start, morning, afternoon, null, "x".repeat(301), null, null, Instant.now(), 1L))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new ClassSession(batch(), start, morning, afternoon, null, null, null, "x".repeat(2001), Instant.now(), 1L))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new BatchSchedule(batch(), 1, morning, afternoon, null, "x".repeat(151), Instant.now(), 1L))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private ClassSession session(LocalDate date) {
        return new ClassSession(batch(), date, morning, afternoon, null, null, null, null, Instant.now(), 1L);
    }
    private CourseBatch batch() {
        var batch = mock(CourseBatch.class);
        when(batch.getStartDate()).thenReturn(start);
        when(batch.getEndDate()).thenReturn(end);
        return batch;
    }
}
