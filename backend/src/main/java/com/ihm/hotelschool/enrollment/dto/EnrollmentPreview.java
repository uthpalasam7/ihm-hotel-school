package com.ihm.hotelschool.enrollment.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record EnrollmentPreview(String currencyCode, BigDecimal totalAmount, List<Charge> charges, Long batchVersion, Long feePlanVersion) {
    public record Charge(String type, Integer installmentNumber, String description, LocalDate dueDate, BigDecimal amount) {}
}
