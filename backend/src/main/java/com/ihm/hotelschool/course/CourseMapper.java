package com.ihm.hotelschool.course;

import com.ihm.hotelschool.course.dto.CourseResponse;
import org.springframework.stereotype.Component;

@Component
class CourseMapper {

	CourseResponse toResponse(Course course, long batchCount) {
		return new CourseResponse(
				course.getId(),
				course.getName(),
				course.getShortCode(),
				course.getDescription(),
				course.getStatus(),
				batchCount,
				course.getCreatedAt(),
				course.getUpdatedAt(),
				course.getVersion());
	}
}
