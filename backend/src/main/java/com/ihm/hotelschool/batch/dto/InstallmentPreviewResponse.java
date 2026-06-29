package com.ihm.hotelschool.batch.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record InstallmentPreviewResponse(
		String currencyCode,
		BigDecimal totalAmount,
		List<PreviewCharge> charges) {

	public record PreviewCharge(
			String type,
			Integer installmentNumber,
			String description,
			LocalDate dueDate,
			BigDecimal amount) {
	}
}
