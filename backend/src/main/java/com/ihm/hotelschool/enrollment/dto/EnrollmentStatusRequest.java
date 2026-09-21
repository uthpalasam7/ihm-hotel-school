package com.ihm.hotelschool.enrollment.dto;

import com.ihm.hotelschool.enrollment.EnrollmentStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record EnrollmentStatusRequest(@NotNull EnrollmentStatus status,
        @NotBlank @Size(max=500) String reason, Long version) {}
