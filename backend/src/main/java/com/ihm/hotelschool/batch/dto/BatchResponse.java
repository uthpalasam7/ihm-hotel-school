package com.ihm.hotelschool.batch.dto;

import com.ihm.hotelschool.batch.BatchStatus;
import com.ihm.hotelschool.batch.ScheduleMode;
import java.time.Instant;
import java.time.LocalDate;

public record BatchResponse(
		Long id,
		BatchCourseResponse course,
		BatchBranchResponse branch,
		String batchNumber,
		LocalDate startDate,
		LocalDate endDate,
		Integer durationMonths,
		ScheduleMode scheduleMode,
		BatchStatus status,
		String remarks,
		Integer registrationSequence,
		long lecturerCount,
		long studentCount,
		boolean feePlanConfigured,
		Instant createdAt,
		Instant updatedAt,
		Long version) {
}
