package com.ihm.hotelschool.session.dto;

import com.ihm.hotelschool.session.ScheduleStatus;
import jakarta.validation.constraints.*;
import java.time.LocalTime;

public record ScheduleRequest(@NotNull @Min(1) @Max(7) Integer dayOfWeek,
        @NotNull LocalTime startTime, @NotNull LocalTime endTime,
        @Positive Long defaultLecturerUserId, @Size(max = 150) String classroom,
        @NotNull ScheduleStatus status, @PositiveOrZero Long version) {}
