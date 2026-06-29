package com.ihm.hotelschool.course.dto;

import com.ihm.hotelschool.course.CourseStatus;
import java.time.Instant;

public record CourseResponse(
		Long id,
		String name,
		String shortCode,
		String description,
		CourseStatus status,
		long batchCount,
		Instant createdAt,
		Instant updatedAt,
		Long version) {
}
