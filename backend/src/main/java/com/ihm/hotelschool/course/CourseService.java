package com.ihm.hotelschool.course;

import com.ihm.hotelschool.audit.AuditService;
import com.ihm.hotelschool.batch.CourseBatchRepository;
import com.ihm.hotelschool.common.security.CurrentActorService;
import com.ihm.hotelschool.common.security.CurrentActorService.CurrentActor;
import com.ihm.hotelschool.common.web.ConflictException;
import com.ihm.hotelschool.common.web.NotFoundException;
import com.ihm.hotelschool.common.web.PageResponse;
import com.ihm.hotelschool.course.dto.CourseRequest;
import com.ihm.hotelschool.course.dto.CourseResponse;
import com.ihm.hotelschool.course.dto.CourseStatusRequest;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Clock;
import java.time.Instant;
import java.util.Locale;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class CourseService {

	private final CourseRepository courseRepository;
	private final CourseBatchRepository batchRepository;
	private final CourseMapper courseMapper;
	private final CurrentActorService currentActorService;
	private final AuditService auditService;
	private final Clock clock;

	CourseService(CourseRepository courseRepository, CourseBatchRepository batchRepository, CourseMapper courseMapper,
			CurrentActorService currentActorService, AuditService auditService, Clock clock) {
		this.courseRepository = courseRepository;
		this.batchRepository = batchRepository;
		this.courseMapper = courseMapper;
		this.currentActorService = currentActorService;
		this.auditService = auditService;
		this.clock = clock;
	}

	@Transactional(readOnly = true)
	PageResponse<CourseResponse> list(String status, String search, Pageable pageable, Authentication authentication) {
		adminActor(authentication);
		Pageable safePageable = PageRequest.of(pageable.getPageNumber(), Math.min(pageable.getPageSize(), 100), Sort.by("name").ascending());
		Specification<Course> spec = Specification.where(null);
		if (status != null && !status.isBlank()) {
			CourseStatus courseStatus = CourseStatus.valueOf(status.trim().toUpperCase(Locale.ROOT));
			spec = spec.and((root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get("status"), courseStatus));
		}
		String normalizedSearch = blankToNull(search);
		if (normalizedSearch != null) {
			String like = "%" + normalizedSearch.toLowerCase(Locale.ROOT) + "%";
			spec = spec.and((root, query, criteriaBuilder) -> criteriaBuilder.or(
					criteriaBuilder.like(criteriaBuilder.lower(root.get("name")), like),
					criteriaBuilder.like(criteriaBuilder.lower(root.get("shortCode")), like)));
		}
		return PageResponse.from(courseRepository.findAll(spec, safePageable), this::toResponse);
	}

	@Transactional(readOnly = true)
	CourseResponse get(Long id, Authentication authentication) {
		adminActor(authentication);
		return toResponse(findCourse(id));
	}

	@Transactional
	CourseResponse create(CourseRequest request, Authentication authentication, HttpServletRequest httpRequest) {
		CurrentActor actor = adminActor(authentication);
		String shortCode = normalizeCode(request.shortCode());
		if (courseRepository.existsByShortCode(shortCode)) {
			throw new ConflictException("Course short code already exists");
		}
		Instant now = clock.instant();
		Course course = courseRepository.save(new Course(
				normalizeRequired(request.name()),
				shortCode,
				blankToNull(request.description()),
				request.status(),
				now,
				actor.id()));
		CourseResponse response = toResponse(course);
		auditService.record(actor.user(), null, "COURSE_CREATED", "Course", course.getId(), null, response, null, httpRequest);
		return response;
	}

	@Transactional
	CourseResponse update(Long id, CourseRequest request, Authentication authentication, HttpServletRequest httpRequest) {
		CurrentActor actor = adminActor(authentication);
		Course course = findCourse(id);
		CourseResponse oldValue = toResponse(course);
		String shortCode = normalizeCode(request.shortCode());
		if (courseRepository.existsByShortCodeAndIdNot(shortCode, id)) {
			throw new ConflictException("Course short code already exists");
		}
		course.updateDetails(
				normalizeRequired(request.name()),
				shortCode,
				blankToNull(request.description()),
				request.status(),
				clock.instant(),
				actor.id());
		CourseResponse response = toResponse(course);
		auditService.record(actor.user(), null, "COURSE_UPDATED", "Course", course.getId(), oldValue, response, null, httpRequest);
		return response;
	}

	@Transactional
	CourseResponse changeStatus(Long id, CourseStatusRequest request, Authentication authentication, HttpServletRequest httpRequest) {
		CurrentActor actor = adminActor(authentication);
		Course course = findCourse(id);
		CourseResponse oldValue = toResponse(course);
		course.changeStatus(request.status(), clock.instant(), actor.id());
		CourseResponse response = toResponse(course);
		auditService.record(actor.user(), null, "COURSE_STATUS_CHANGED", "Course", course.getId(), oldValue, response, request.reason(), httpRequest);
		return response;
	}

	private CourseResponse toResponse(Course course) {
		return courseMapper.toResponse(course, batchRepository.countByCourseId(course.getId()));
	}

	private CurrentActor adminActor(Authentication authentication) {
		CurrentActor actor = currentActorService.actor(authentication);
		actor.requireAnyRole("SUPER_ADMIN", "ADMIN");
		return actor;
	}

	private Course findCourse(Long id) {
		return courseRepository.findById(id).orElseThrow(() -> new NotFoundException("Course was not found"));
	}

	private String normalizeCode(String value) {
		return normalizeRequired(value).toUpperCase(Locale.ROOT);
	}

	private String normalizeRequired(String value) {
		return value.trim();
	}

	private String blankToNull(String value) {
		String normalized = value == null ? null : value.trim();
		return normalized == null || normalized.isBlank() ? null : normalized;
	}
}
