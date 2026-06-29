package com.ihm.hotelschool.batch;

import com.ihm.hotelschool.batch.dto.BatchBranchResponse;
import com.ihm.hotelschool.batch.dto.BatchCourseResponse;
import com.ihm.hotelschool.batch.dto.BatchLecturerResponse;
import com.ihm.hotelschool.batch.dto.BatchResponse;
import com.ihm.hotelschool.batch.dto.FeePlanResponse;
import org.springframework.stereotype.Component;

@Component
class BatchMapper {

	BatchResponse toResponse(CourseBatch batch, long lecturerCount, long studentCount, boolean feePlanConfigured) {
		return new BatchResponse(
				batch.getId(),
				new BatchCourseResponse(batch.getCourse().getId(), batch.getCourse().getName(), batch.getCourse().getShortCode()),
				new BatchBranchResponse(batch.getBranch().getId(), batch.getBranch().getCode(), batch.getBranch().getName()),
				batch.getBatchNumber(),
				batch.getStartDate(),
				batch.getEndDate(),
				batch.getDurationMonths(),
				batch.getScheduleMode(),
				batch.getStatus(),
				batch.getRemarks(),
				batch.getRegistrationSequence(),
				lecturerCount,
				studentCount,
				feePlanConfigured,
				batch.getCreatedAt(),
				batch.getUpdatedAt(),
				batch.getVersion());
	}

	FeePlanResponse toResponse(FeePlan feePlan) {
		return new FeePlanResponse(
				feePlan.getId(),
				feePlan.getBatch().getId(),
				feePlan.getCurrencyCode(),
				feePlan.getRegistrationFee(),
				feePlan.getCourseFee(),
				feePlan.getExaminationFee(),
				feePlan.getDurationMonths(),
				feePlan.getMonthlyDueDay(),
				feePlan.getExaminationDueDate(),
				feePlan.getStatus(),
				feePlan.getCreatedAt(),
				feePlan.getUpdatedAt(),
				feePlan.getVersion());
	}

	BatchLecturerResponse toResponse(BatchLecturer assignment) {
		return new BatchLecturerResponse(
				assignment.getId(),
				assignment.getBatch().getId(),
				assignment.getLecturer().getId(),
				assignment.getLecturer().getFullName(),
				assignment.getLecturer().getUsername(),
				assignment.getAssignmentStartDate(),
				assignment.getAssignmentEndDate(),
				assignment.getStatus(),
				assignment.getCreatedAt(),
				assignment.getUpdatedAt());
	}
}
