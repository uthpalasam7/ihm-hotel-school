package com.ihm.hotelschool.batch.dto;

import com.ihm.hotelschool.batch.BatchLecturerStatus;
import java.time.Instant;
import java.time.LocalDate;

public record BatchLecturerResponse(
		Long id,
		Long batchId,
		Long lecturerUserId,
		String lecturerFullName,
		String lecturerUsername,
		LocalDate assignmentStartDate,
		LocalDate assignmentEndDate,
		BatchLecturerStatus status,
		Instant createdAt,
		Instant updatedAt) {
}
