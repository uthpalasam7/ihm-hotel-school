package com.ihm.hotelschool.session.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public record GenerationPreview(String previewToken, Instant expiresAt, int createCount, int skipCount,
        int conflictCount, List<Entry> sessions) {
    public enum Outcome { CREATE, ALREADY_GENERATED, EXISTING, CONFLICT }
    public record Entry(Long scheduleId, LocalDate sessionDate, LocalTime startTime, LocalTime endTime,
            Long lecturerUserId, String lecturerName, String classroom, Outcome outcome,
            Long existingSessionId, String message) {}
}
