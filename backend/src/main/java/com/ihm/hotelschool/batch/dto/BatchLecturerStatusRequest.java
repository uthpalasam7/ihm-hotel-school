package com.ihm.hotelschool.batch.dto;

import com.ihm.hotelschool.batch.BatchLecturerStatus;
import jakarta.validation.constraints.NotNull;

public record BatchLecturerStatusRequest(
		@NotNull BatchLecturerStatus status,
		String reason) {
}
