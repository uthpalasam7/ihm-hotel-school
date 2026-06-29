package com.ihm.hotelschool.batch.dto;

import com.ihm.hotelschool.batch.FeePlanStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public record FeePlanResponse(
		Long id,
		Long batchId,
		String currencyCode,
		BigDecimal registrationFee,
		BigDecimal courseFee,
		BigDecimal examinationFee,
		Integer durationMonths,
		Integer monthlyDueDay,
		LocalDate examinationDueDate,
		FeePlanStatus status,
		Instant createdAt,
		Instant updatedAt,
		Long version) {
}
