package com.ihm.hotelschool.enrollment.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record EnrollmentRequest(@NotNull @Positive Long studentId, @NotNull @Positive Long batchId,
        @NotNull LocalDate enrollmentDate, @Size(max = 2000) String remarks, Long expectedBatchVersion, Long expectedFeePlanVersion) {}
