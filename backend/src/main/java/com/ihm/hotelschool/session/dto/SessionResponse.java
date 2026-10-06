package com.ihm.hotelschool.session.dto;

import com.ihm.hotelschool.session.SessionStatus;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;

public record SessionResponse(Long id, Long batchId, String batchNumber, String courseName,
        Long branchId, String branchName, LocalDate sessionDate, LocalTime startTime, LocalTime endTime,
        Long lecturerUserId, String lecturerName, String topic, String classroom, SessionStatus status,
        String cancellationReason, Long originalSessionId, String remarks, Instant attendanceSubmittedAt,
        Instant createdAt, Instant updatedAt, Long version, Long sourceScheduleId, LocalDate generationDate, String reschedulingReason) {}
