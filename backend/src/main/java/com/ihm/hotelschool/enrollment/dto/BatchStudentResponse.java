package com.ihm.hotelschool.enrollment.dto;

import com.ihm.hotelschool.enrollment.EnrollmentStatus;
import java.time.LocalDate;

public record BatchStudentResponse(Long studentId, String studentName,
        String registrationNumber, LocalDate enrollmentDate, EnrollmentStatus status) {}
