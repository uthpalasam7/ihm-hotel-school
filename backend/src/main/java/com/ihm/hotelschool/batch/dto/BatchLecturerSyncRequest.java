package com.ihm.hotelschool.batch.dto;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.List;

public record BatchLecturerSyncRequest(
		@NotNull List<@NotNull Long> lecturerUserIds,
		LocalDate assignmentStartDate,
		LocalDate assignmentEndDate) {
}
