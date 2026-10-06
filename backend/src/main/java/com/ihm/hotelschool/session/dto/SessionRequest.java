package com.ihm.hotelschool.session.dto;

import jakarta.validation.constraints.*;
import java.time.LocalDate;
import java.time.LocalTime;

public record SessionRequest(@NotNull @Positive Long batchId,
        @NotNull LocalDate sessionDate, @NotNull LocalTime startTime, @NotNull LocalTime endTime,
        @Positive Long lecturerUserId, @Size(max = 300) String topic,
        @Size(max = 150) String classroom, @Size(max = 2000) String remarks,
        @PositiveOrZero Long version) {}
