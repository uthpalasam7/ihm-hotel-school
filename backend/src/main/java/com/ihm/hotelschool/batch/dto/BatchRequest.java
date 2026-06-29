package com.ihm.hotelschool.batch.dto;

import com.ihm.hotelschool.batch.BatchStatus;
import com.ihm.hotelschool.batch.ScheduleMode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record BatchRequest(
		@NotNull Long courseId,
		@NotNull Long branchId,
		@NotBlank @Size(max = 60) String batchNumber,
		@NotNull LocalDate startDate,
		@NotNull LocalDate endDate,
		@NotNull @Positive Integer durationMonths,
		@NotNull ScheduleMode scheduleMode,
		@NotNull BatchStatus status,
		String remarks) {
}
