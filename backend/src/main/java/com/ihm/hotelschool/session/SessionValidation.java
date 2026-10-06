package com.ihm.hotelschool.session;

import com.ihm.hotelschool.batch.CourseBatch;
import java.time.LocalDate;
import java.time.LocalTime;

final class SessionValidation {
    private SessionValidation() {}

    static void times(LocalTime start, LocalTime end) {
        if (start == null || end == null || !start.isBefore(end)) {
            throw new IllegalArgumentException("Session start time must be before end time");
        }
    }

    static void date(CourseBatch batch, LocalDate date) {
        if (batch == null || date == null || date.isBefore(batch.getStartDate()) || date.isAfter(batch.getEndDate())) {
            throw new IllegalArgumentException("Session date must be within the batch dates");
        }
    }

    static String text(String value, int max, String field) {
        if (value == null || value.isBlank()) return null;
        String trimmed = value.trim();
        if (trimmed.length() > max) throw new IllegalArgumentException(field + " is too long");
        return trimmed;
    }
}
