package com.ihm.hotelschool.course.dto;

import com.ihm.hotelschool.course.CourseStatus;
import jakarta.validation.constraints.NotNull;

public record CourseStatusRequest(
		@NotNull CourseStatus status,
		String reason) {
}
