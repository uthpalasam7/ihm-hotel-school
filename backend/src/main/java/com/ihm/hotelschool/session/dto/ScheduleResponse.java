package com.ihm.hotelschool.session.dto;

import com.ihm.hotelschool.session.ScheduleStatus;
import java.time.LocalTime;

public record ScheduleResponse(Long id, Long batchId, int dayOfWeek, LocalTime startTime,
        LocalTime endTime, Long defaultLecturerUserId, String defaultLecturerName, String classroom,
        ScheduleStatus status, Long version) {}
