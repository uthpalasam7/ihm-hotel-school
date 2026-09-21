package com.ihm.hotelschool.enrollment.dto;

import com.ihm.hotelschool.enrollment.EnrollmentStatus;
import java.time.Instant;
import java.time.LocalDate;

public record EnrollmentResponse(Long id, Long studentId, String studentName, Long batchId,
        String batchNumber, String courseName, Long branchId, String branchName,
        String registrationNumber, LocalDate enrollmentDate, EnrollmentStatus status,
        String remarks, Instant createdAt, Long version) {}
