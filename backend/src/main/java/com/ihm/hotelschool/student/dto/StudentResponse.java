package com.ihm.hotelschool.student.dto;

import com.ihm.hotelschool.student.StudentStatus;
import java.time.Instant;
import java.time.LocalDate;

public record StudentResponse(
		Long id,
		String fullName,
		String nic,
		String contactNumber,
		String alternativeContactNumber,
		String email,
		String address,
		LocalDate dateOfBirth,
		String gender,
		String remarks,
		StudentStatus status,
		boolean photoAvailable,
		String photoUrl,
		String photoThumbnailUrl,
		Instant createdAt,
		Instant updatedAt,
		Long version) {
}
