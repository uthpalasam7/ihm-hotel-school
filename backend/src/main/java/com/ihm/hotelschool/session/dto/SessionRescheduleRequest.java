package com.ihm.hotelschool.session.dto;

import jakarta.validation.constraints.*;
import java.time.LocalDate;
import java.time.LocalTime;

public record SessionRescheduleRequest(@NotNull LocalDate newDate,
        @NotNull LocalTime newStartTime, @NotNull LocalTime newEndTime,
        @Positive Long lecturerUserId, @NotBlank @Size(max = 2000) String reason,
        @NotNull @PositiveOrZero Long version) {}
