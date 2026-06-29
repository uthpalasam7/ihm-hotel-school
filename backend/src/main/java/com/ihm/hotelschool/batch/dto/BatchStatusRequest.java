package com.ihm.hotelschool.batch.dto;

import com.ihm.hotelschool.batch.BatchStatus;
import jakarta.validation.constraints.NotNull;

public record BatchStatusRequest(
		@NotNull BatchStatus status,
		String reason) {
}
