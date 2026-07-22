package com.ihm.hotelschool.student;

import com.ihm.hotelschool.student.dto.StudentResponse;
import org.springframework.stereotype.Component;

@Component
class StudentMapper {

	StudentResponse toResponse(Student student) {
		boolean hasPhoto = student.getPhotoStorageKey() != null;
		String baseUrl = "/api/v1/students/" + student.getId() + "/photo";
		return new StudentResponse(
				student.getId(),
				student.getFullName(),
				student.getNic(),
				student.getContactNumber(),
				student.getAlternativeContactNumber(),
				student.getEmail(),
				student.getAddress(),
				student.getDateOfBirth(),
				student.getGender(),
				student.getRemarks(),
				student.getStatus(),
				hasPhoto,
				hasPhoto ? baseUrl : null,
				hasPhoto ? baseUrl + "?variant=thumbnail" : null,
				student.getCreatedAt(),
				student.getUpdatedAt(),
				student.getVersion());
	}
}
