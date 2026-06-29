package com.ihm.hotelschool.batch.dto;

import com.ihm.hotelschool.batch.FeePlanStatus;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;

public record FeePlanRequest(
		@DecimalMin("0.00") BigDecimal registrationFee,
		@DecimalMin("0.00") BigDecimal courseFee,
		@DecimalMin("0.00") BigDecimal examinationFee,
		@NotNull @Positive Integer durationMonths,
		@NotNull @Min(1) @Max(31) Integer monthlyDueDay,
		@NotNull LocalDate examinationDueDate,
		@Size(min = 3, max = 3) String currencyCode,
		FeePlanStatus status) {
}
