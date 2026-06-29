package com.ihm.hotelschool.batch.dto;

import com.ihm.hotelschool.batch.BatchLecturerStatus;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record BatchLecturerRequest(
		@NotNull Long lecturerUserId,
		@NotNull LocalDate assignmentStartDate,
		LocalDate assignmentEndDate,
		@NotNull BatchLecturerStatus status) {
}
