package com.ihm.hotelschool.course;

import com.ihm.hotelschool.common.web.PageResponse;
import com.ihm.hotelschool.course.dto.CourseRequest;
import com.ihm.hotelschool.course.dto.CourseResponse;
import com.ihm.hotelschool.course.dto.CourseStatusRequest;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/courses")
class CourseController {

	private final CourseService courseService;

	CourseController(CourseService courseService) {
		this.courseService = courseService;
	}

	@GetMapping
	PageResponse<CourseResponse> list(
			@RequestParam(required = false) String status,
			@RequestParam(required = false) String search,
			Pageable pageable,
			Authentication authentication) {
		return courseService.list(status, search, pageable, authentication);
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	CourseResponse create(
			@Valid @RequestBody CourseRequest request,
			Authentication authentication,
			HttpServletRequest httpRequest) {
		return courseService.create(request, authentication, httpRequest);
	}

	@GetMapping("/{id}")
	CourseResponse get(@PathVariable Long id, Authentication authentication) {
		return courseService.get(id, authentication);
	}

	@PutMapping("/{id}")
	CourseResponse update(
			@PathVariable Long id,
			@Valid @RequestBody CourseRequest request,
			Authentication authentication,
			HttpServletRequest httpRequest) {
		return courseService.update(id, request, authentication, httpRequest);
	}

	@PatchMapping("/{id}/status")
	CourseResponse changeStatus(
			@PathVariable Long id,
			@Valid @RequestBody CourseStatusRequest request,
			Authentication authentication,
			HttpServletRequest httpRequest) {
		return courseService.changeStatus(id, request, authentication, httpRequest);
	}
}
