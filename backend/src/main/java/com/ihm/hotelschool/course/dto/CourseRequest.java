package com.ihm.hotelschool.course.dto;

import com.ihm.hotelschool.course.CourseStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CourseRequest(
		@NotBlank @Size(max = 200) String name,
		@NotBlank @Size(max = 20) String shortCode,
		String description,
		@NotNull CourseStatus status) {
}
