package com.ihm.hotelschool.batch;

import com.ihm.hotelschool.audit.AuditService;
import com.ihm.hotelschool.batch.dto.BatchLecturerRequest;
import com.ihm.hotelschool.batch.dto.BatchLecturerResponse;
import com.ihm.hotelschool.batch.dto.BatchLecturerStatusRequest;
import com.ihm.hotelschool.batch.dto.BatchLecturerSyncRequest;
import com.ihm.hotelschool.batch.dto.BatchRequest;
import com.ihm.hotelschool.batch.dto.BatchResponse;
import com.ihm.hotelschool.batch.dto.BatchStatusRequest;
import com.ihm.hotelschool.batch.dto.FeePlanRequest;
import com.ihm.hotelschool.batch.dto.FeePlanResponse;
import com.ihm.hotelschool.batch.dto.InstallmentPreviewResponse;
import com.ihm.hotelschool.branch.Branch;
import com.ihm.hotelschool.branch.BranchRepository;
import com.ihm.hotelschool.branch.BranchStatus;
import com.ihm.hotelschool.common.security.CurrentActorService;
import com.ihm.hotelschool.common.security.CurrentActorService.CurrentActor;
import com.ihm.hotelschool.common.web.ConflictException;
import com.ihm.hotelschool.common.web.NotFoundException;
import com.ihm.hotelschool.common.web.PageResponse;
import com.ihm.hotelschool.course.Course;
import com.ihm.hotelschool.course.CourseRepository;
import com.ihm.hotelschool.enrollment.EnrollmentRepository;
import com.ihm.hotelschool.user.Role;
import com.ihm.hotelschool.user.UserAccount;
import com.ihm.hotelschool.user.UserRepository;
import com.ihm.hotelschool.user.UserStatus;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class BatchService {

	private static final String DEFAULT_CURRENCY = "LKR";

	private final CourseBatchRepository batchRepository;
	private final EnrollmentRepository enrollmentRepository;
	private final CourseRepository courseRepository;
	private final BranchRepository branchRepository;
	private final FeePlanRepository feePlanRepository;
	private final BatchLecturerRepository batchLecturerRepository;
	private final UserRepository userRepository;
	private final BatchMapper batchMapper;
	private final CurrentActorService currentActorService;
	private final AuditService auditService;
	private final Clock clock;
	private final java.time.ZoneId zone;

	BatchService(
			CourseBatchRepository batchRepository,
			EnrollmentRepository enrollmentRepository,
			CourseRepository courseRepository,
			BranchRepository branchRepository,
			FeePlanRepository feePlanRepository,
			BatchLecturerRepository batchLecturerRepository,
			UserRepository userRepository,
			BatchMapper batchMapper,
			CurrentActorService currentActorService,
			AuditService auditService,
			Clock clock,
			@org.springframework.beans.factory.annotation.Value("${app.timezone:Asia/Colombo}") String timezone) {
		this.batchRepository = batchRepository;
		this.enrollmentRepository = enrollmentRepository;
		this.courseRepository = courseRepository;
		this.branchRepository = branchRepository;
		this.feePlanRepository = feePlanRepository;
		this.batchLecturerRepository = batchLecturerRepository;
		this.userRepository = userRepository;
		this.batchMapper = batchMapper;
		this.currentActorService = currentActorService;
		this.auditService = auditService;
		this.clock = clock;
		this.zone = java.time.ZoneId.of(timezone);
	}

	@Transactional(readOnly = true)
	PageResponse<BatchResponse> list(Long branchId, Long courseId, Long lecturerId, String status, LocalDate startDateFrom,
			LocalDate startDateTo, String search, Pageable pageable, Authentication authentication) {
		CurrentActor actor = currentActorService.actor(authentication);
		actor.requireAnyRole("SUPER_ADMIN", "ADMIN", "LECTURER");
		if (branchId != null) {
			actor.requireBranchAccess(Set.of(branchId));
		}
		Pageable safePageable = PageRequest.of(pageable.getPageNumber(), Math.min(pageable.getPageSize(), 100), Sort.by("startDate").descending());
		Specification<CourseBatch> spec = Specification.where(null);
		if (!actor.superAdmin()) {
			Set<Long> actorBranchIds = actor.branchIds();
			spec = spec.and((root, query, criteriaBuilder) -> root.join("branch", JoinType.INNER).get("id").in(actorBranchIds));
		}
		if (actor.hasRole("LECTURER") && !actor.hasRole("ADMIN") && !actor.superAdmin()) {
			spec = spec.and((root, query, criteriaBuilder) -> {
				query.distinct(true);
				Join<CourseBatch, BatchLecturer> assignment = root.join("lecturerAssignments", JoinType.INNER);
				return criteriaBuilder.and(
						criteriaBuilder.equal(assignment.get("lecturer").get("id"), actor.id()),
						criteriaBuilder.equal(assignment.get("status"), BatchLecturerStatus.ACTIVE),
						criteriaBuilder.lessThanOrEqualTo(assignment.get("assignmentStartDate"), LocalDate.now(clock.withZone(zone))),
						criteriaBuilder.or(criteriaBuilder.isNull(assignment.get("assignmentEndDate")),
								criteriaBuilder.greaterThanOrEqualTo(assignment.get("assignmentEndDate"), LocalDate.now(clock.withZone(zone)))));
			});
		}
		if (branchId != null) {
			spec = spec.and((root, query, criteriaBuilder) -> criteriaBuilder.equal(root.join("branch", JoinType.INNER).get("id"), branchId));
		}
		if (courseId != null) {
			spec = spec.and((root, query, criteriaBuilder) -> criteriaBuilder.equal(root.join("course", JoinType.INNER).get("id"), courseId));
		}
		if (lecturerId != null) {
			spec = spec.and((root, query, criteriaBuilder) -> {
				query.distinct(true);
				Join<CourseBatch, BatchLecturer> assignment = root.join("lecturerAssignments", JoinType.INNER);
				return criteriaBuilder.equal(assignment.get("lecturer").get("id"), lecturerId);
			});
		}
		if (status != null && !status.isBlank()) {
			BatchStatus batchStatus = BatchStatus.valueOf(status.trim().toUpperCase(Locale.ROOT));
			spec = spec.and((root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get("status"), batchStatus));
		}
		if (startDateFrom != null) {
			spec = spec.and((root, query, criteriaBuilder) -> criteriaBuilder.greaterThanOrEqualTo(root.get("startDate"), startDateFrom));
		}
		if (startDateTo != null) {
			spec = spec.and((root, query, criteriaBuilder) -> criteriaBuilder.lessThanOrEqualTo(root.get("startDate"), startDateTo));
		}
		String normalizedSearch = blankToNull(search);
		if (normalizedSearch != null) {
			String like = "%" + normalizedSearch.toLowerCase(Locale.ROOT) + "%";
			spec = spec.and((root, query, criteriaBuilder) -> criteriaBuilder.or(
					criteriaBuilder.like(criteriaBuilder.lower(root.get("batchNumber")), like),
					criteriaBuilder.like(criteriaBuilder.lower(root.join("course", JoinType.INNER).get("name")), like)));
		}
		return PageResponse.from(batchRepository.findAll(spec, safePageable), this::toResponse);
	}

	@Transactional(readOnly = true)
	BatchResponse get(Long id, Authentication authentication) {
		CurrentActor actor = currentActorService.actor(authentication);
		actor.requireAnyRole("SUPER_ADMIN", "ADMIN", "LECTURER");
		CourseBatch batch = findBatch(id);
		actor.requireBranchAccess(Set.of(batch.getBranch().getId()));
		if (actor.hasRole("LECTURER") && !actor.hasRole("ADMIN") && !actor.superAdmin()) {
			boolean assigned = batchLecturerRepository.hasActiveAssignmentOn(batch.getId(), actor.id(),
					LocalDate.now(clock.withZone(zone)));
			if (!assigned) {
				throw new AccessDeniedException("Access denied");
			}
		}
		return toResponse(batch);
	}

	@Transactional
	BatchResponse create(BatchRequest request, Authentication authentication, HttpServletRequest httpRequest) {
		CurrentActor actor = adminActor(authentication);
		Course course = findCourse(request.courseId());
		Branch branch = resolveAuthorizedBranch(request.branchId(), actor);
		validateBatchRequest(request);
		String batchNumber = normalizeRequired(request.batchNumber());
		if (batchRepository.existsByBatchNumber(batchNumber)) {
			throw new ConflictException("Batch number already exists");
		}
		Instant now = clock.instant();
		CourseBatch batch = batchRepository.save(new CourseBatch(
				course,
				branch,
				batchNumber,
				request.startDate(),
				request.endDate(),
				request.durationMonths(),
				request.scheduleMode(),
				request.status(),
				blankToNull(request.remarks()),
				now,
				actor.id()));
		BatchResponse response = toResponse(batch);
		auditService.record(actor.user(), branch, "BATCH_CREATED", "CourseBatch", batch.getId(), null, response, null, httpRequest);
		return response;
	}

	@Transactional
	BatchResponse update(Long id, BatchRequest request, Authentication authentication, HttpServletRequest httpRequest) {
		CurrentActor actor = adminActor(authentication);
		CourseBatch batch = findBatch(id);
		actor.requireBranchAccess(Set.of(batch.getBranch().getId()));
		Course course = findCourse(request.courseId());
		Branch branch = resolveAuthorizedBranch(request.branchId(), actor);
		validateBatchRequest(request);
		String batchNumber = normalizeRequired(request.batchNumber());
        if (enrollmentRepository.existsByBatchId(id) && (
                !batch.getCourse().getId().equals(course.getId())
                || !batch.getBranch().getId().equals(branch.getId())
                || !batch.getBatchNumber().equals(batchNumber))) {
            throw new ConflictException("Course, branch, and batch number cannot change after enrollment");
        }
		if (batchRepository.existsByBatchNumberAndIdNot(batchNumber, id)) {
			throw new ConflictException("Batch number already exists");
		}
		BatchResponse oldValue = toResponse(batch);
		batch.updateDetails(course, branch, batchNumber, request.startDate(), request.endDate(), request.durationMonths(),
				request.scheduleMode(), request.status(), blankToNull(request.remarks()), clock.instant(), actor.id());
		BatchResponse response = toResponse(batch);
		auditService.record(actor.user(), branch, "BATCH_UPDATED", "CourseBatch", batch.getId(), oldValue, response, null, httpRequest);
		return response;
	}

	@Transactional
	BatchResponse changeStatus(Long id, BatchStatusRequest request, Authentication authentication, HttpServletRequest httpRequest) {
		CurrentActor actor = adminActor(authentication);
		CourseBatch batch = findBatch(id);
		actor.requireBranchAccess(Set.of(batch.getBranch().getId()));
		BatchResponse oldValue = toResponse(batch);
		batch.changeStatus(request.status(), clock.instant(), actor.id());
		BatchResponse response = toResponse(batch);
		auditService.record(actor.user(), batch.getBranch(), "BATCH_STATUS_CHANGED", "CourseBatch", batch.getId(), oldValue, response, request.reason(), httpRequest);
		return response;
	}

	@Transactional(readOnly = true)
	FeePlanResponse getFeePlan(Long batchId, Authentication authentication) {
		CourseBatch batch = requireBatchManageAccess(batchId, authentication);
		return batchMapper.toResponse(feePlanRepository.findByBatchId(batch.getId()).orElseThrow(() -> new NotFoundException("Fee plan was not found")));
	}

	@Transactional
	FeePlanResponse saveFeePlan(Long batchId, FeePlanRequest request, Authentication authentication, HttpServletRequest httpRequest) {
		CurrentActor actor = adminActor(authentication);
		CourseBatch batch = findBatch(batchId);
		actor.requireBranchAccess(Set.of(batch.getBranch().getId()));
		validateFeePlan(request);
		String currencyCode = normalizeCurrency(request.currencyCode());
		FeePlanStatus status = request.status() == null ? FeePlanStatus.ACTIVE : request.status();
		Instant now = clock.instant();
		FeePlan feePlan = feePlanRepository.findByBatchId(batch.getId()).orElse(null);
		FeePlanResponse oldValue = feePlan == null ? null : batchMapper.toResponse(feePlan);
		if (feePlan == null) {
			feePlan = new FeePlan(batch, currencyCode, money(request.registrationFee()), money(request.courseFee()),
					money(request.examinationFee()), request.durationMonths(), request.monthlyDueDay(),
					request.examinationDueDate(), status, now, actor.id());
			feePlanRepository.save(feePlan);
		} else {
			feePlan.updateDetails(currencyCode, money(request.registrationFee()), money(request.courseFee()),
					money(request.examinationFee()), request.durationMonths(), request.monthlyDueDay(),
					request.examinationDueDate(), status, now, actor.id());
		}
		FeePlanResponse response = batchMapper.toResponse(feePlan);
		auditService.record(actor.user(), batch.getBranch(), "FEE_PLAN_CHANGED", "FeePlan", feePlan.getId(), oldValue, response, null, httpRequest);
		return response;
	}

	@Transactional(readOnly = true)
	InstallmentPreviewResponse previewFeePlan(Long batchId, FeePlanRequest request, Authentication authentication) {
		CourseBatch batch = requireBatchManageAccess(batchId, authentication);
		validateFeePlan(request);
		return preview(batch, request);
	}

	@Transactional(readOnly = true)
	List<BatchLecturerResponse> listLecturers(Long batchId, Authentication authentication) {
		CourseBatch batch = requireBatchManageAccess(batchId, authentication);
		return batchLecturerRepository.findByBatchIdOrderByAssignmentStartDateAsc(batch.getId()).stream()
				.map(batchMapper::toResponse)
				.toList();
	}

	@Transactional
	BatchLecturerResponse addLecturer(Long batchId, BatchLecturerRequest request, Authentication authentication, HttpServletRequest httpRequest) {
		CurrentActor actor = adminActor(authentication);
		CourseBatch batch = findBatch(batchId);
		actor.requireBranchAccess(Set.of(batch.getBranch().getId()));
		UserAccount lecturer = resolveLecturer(request.lecturerUserId(), batch.getBranch().getId());
		validateLecturerRequest(request);
		if (request.status() == BatchLecturerStatus.ACTIVE
				&& batchLecturerRepository.findByBatchIdAndLecturerIdAndStatus(batch.getId(), lecturer.getId(), BatchLecturerStatus.ACTIVE).isPresent()) {
			throw new ConflictException("Lecturer is already actively assigned to this batch");
		}
		Instant now = clock.instant();
		BatchLecturer assignment = batchLecturerRepository.save(new BatchLecturer(batch, lecturer, request.assignmentStartDate(),
				request.assignmentEndDate(), request.status(), now, actor.id()));
		BatchLecturerResponse response = batchMapper.toResponse(assignment);
		auditService.record(actor.user(), batch.getBranch(), "BATCH_LECTURER_ASSIGNED", "BatchLecturer", assignment.getId(), null, response, null, httpRequest);
		return response;
	}

	@Transactional
	BatchLecturerResponse updateLecturer(Long batchId, Long assignmentId, BatchLecturerRequest request, Authentication authentication, HttpServletRequest httpRequest) {
		CurrentActor actor = adminActor(authentication);
		CourseBatch batch = findBatch(batchId);
		actor.requireBranchAccess(Set.of(batch.getBranch().getId()));
		BatchLecturer assignment = findAssignment(batchId, assignmentId);
		UserAccount lecturer = resolveLecturer(request.lecturerUserId(), batch.getBranch().getId());
		validateLecturerRequest(request);
		if (request.status() == BatchLecturerStatus.ACTIVE) {
			batchLecturerRepository.findByBatchIdAndLecturerIdAndStatus(batch.getId(), lecturer.getId(), BatchLecturerStatus.ACTIVE)
					.filter(activeAssignment -> !activeAssignment.getId().equals(assignment.getId()))
					.ifPresent(activeAssignment -> {
						throw new ConflictException("Lecturer is already actively assigned to this batch");
					});
		}
		BatchLecturerResponse oldValue = batchMapper.toResponse(assignment);
		assignment.updateDetails(lecturer, request.assignmentStartDate(), request.assignmentEndDate(), request.status(), clock.instant(), actor.id());
		BatchLecturerResponse response = batchMapper.toResponse(assignment);
		auditService.record(actor.user(), batch.getBranch(), "BATCH_LECTURER_UPDATED", "BatchLecturer", assignment.getId(), oldValue, response, null, httpRequest);
		return response;
	}

	@Transactional
	List<BatchLecturerResponse> syncLecturers(Long batchId, BatchLecturerSyncRequest request, Authentication authentication, HttpServletRequest httpRequest) {
		CurrentActor actor = adminActor(authentication);
		CourseBatch batch = findBatch(batchId);
		actor.requireBranchAccess(Set.of(batch.getBranch().getId()));
		LinkedHashSet<Long> selectedLecturerIds = request.lecturerUserIds().stream()
				.collect(Collectors.toCollection(LinkedHashSet::new));
		if (!selectedLecturerIds.isEmpty() && request.assignmentStartDate() == null) {
			throw new IllegalArgumentException("Assignment start date is required when lecturers are selected");
		}
		if (request.assignmentEndDate() != null && request.assignmentStartDate() != null
				&& request.assignmentEndDate().isBefore(request.assignmentStartDate())) {
			throw new IllegalArgumentException("Assignment end date must not be before the start date");
		}
		Map<Long, UserAccount> selectedLecturers = new LinkedHashMap<>();
		for (Long lecturerUserId : selectedLecturerIds) {
			selectedLecturers.put(lecturerUserId, resolveLecturer(lecturerUserId, batch.getBranch().getId()));
		}

		List<BatchLecturer> currentAssignments = batchLecturerRepository.findByBatchIdOrderByAssignmentStartDateAsc(batch.getId());
		Map<Long, BatchLecturer> activeByLecturerId = currentAssignments.stream()
				.filter(assignment -> assignment.getStatus() == BatchLecturerStatus.ACTIVE)
				.collect(Collectors.toMap(
						assignment -> assignment.getLecturer().getId(),
						Function.identity(),
						(first, duplicate) -> first,
						LinkedHashMap::new));
		Instant now = clock.instant();
		for (BatchLecturer assignment : activeByLecturerId.values()) {
			if (!selectedLecturerIds.contains(assignment.getLecturer().getId())) {
				BatchLecturerResponse oldValue = batchMapper.toResponse(assignment);
				assignment.changeStatus(BatchLecturerStatus.INACTIVE, now, actor.id());
				BatchLecturerResponse response = batchMapper.toResponse(assignment);
				auditService.record(actor.user(), batch.getBranch(), "BATCH_LECTURER_STATUS_CHANGED", "BatchLecturer",
						assignment.getId(), oldValue, response, "Removed from batch lecturer selection", httpRequest);
			}
		}
		for (UserAccount lecturer : selectedLecturers.values()) {
			if (activeByLecturerId.containsKey(lecturer.getId())) {
				continue;
			}
			BatchLecturer assignment = batchLecturerRepository.save(new BatchLecturer(batch, lecturer, request.assignmentStartDate(),
					request.assignmentEndDate(), BatchLecturerStatus.ACTIVE, now, actor.id()));
			BatchLecturerResponse response = batchMapper.toResponse(assignment);
			auditService.record(actor.user(), batch.getBranch(), "BATCH_LECTURER_ASSIGNED", "BatchLecturer", assignment.getId(),
					null, response, null, httpRequest);
		}
		return batchLecturerRepository.findByBatchIdAndStatusOrderByAssignmentStartDateAsc(batch.getId(), BatchLecturerStatus.ACTIVE).stream()
				.map(batchMapper::toResponse)
				.toList();
	}

	@Transactional
	BatchLecturerResponse changeLecturerStatus(Long batchId, Long assignmentId, BatchLecturerStatusRequest request, Authentication authentication, HttpServletRequest httpRequest) {
		CurrentActor actor = adminActor(authentication);
		CourseBatch batch = findBatch(batchId);
		actor.requireBranchAccess(Set.of(batch.getBranch().getId()));
		BatchLecturer assignment = findAssignment(batchId, assignmentId);
		BatchLecturerResponse oldValue = batchMapper.toResponse(assignment);
		assignment.changeStatus(request.status(), clock.instant(), actor.id());
		BatchLecturerResponse response = batchMapper.toResponse(assignment);
		auditService.record(actor.user(), batch.getBranch(), "BATCH_LECTURER_STATUS_CHANGED", "BatchLecturer", assignment.getId(), oldValue, response, request.reason(), httpRequest);
		return response;
	}

	private InstallmentPreviewResponse preview(CourseBatch batch, FeePlanRequest request) {
		String currencyCode = normalizeCurrency(request.currencyCode());
		BigDecimal registrationFee = money(request.registrationFee());
		BigDecimal courseFee = money(request.courseFee());
		BigDecimal examinationFee = money(request.examinationFee());
		BigDecimal baseInstallment = courseFee.divide(BigDecimal.valueOf(request.durationMonths()), 2, RoundingMode.DOWN);
		BigDecimal allocatedBeforeFinal = baseInstallment.multiply(BigDecimal.valueOf(request.durationMonths() - 1L));
		BigDecimal finalInstallment = courseFee.subtract(allocatedBeforeFinal).setScale(2, RoundingMode.UNNECESSARY);
		List<InstallmentPreviewResponse.PreviewCharge> charges = new ArrayList<>();
		charges.add(new InstallmentPreviewResponse.PreviewCharge("REGISTRATION_FEE", null, "Registration fee", batch.getStartDate(), registrationFee));
		for (int i = 1; i <= request.durationMonths(); i++) {
			BigDecimal amount = i == request.durationMonths() ? finalInstallment : baseInstallment;
			charges.add(new InstallmentPreviewResponse.PreviewCharge("COURSE_INSTALLMENT", i, "Course installment " + i, installmentDueDate(batch, request, i), amount));
		}
		charges.add(new InstallmentPreviewResponse.PreviewCharge("EXAMINATION_FEE", null, "Examination fee", request.examinationDueDate(), examinationFee));
		return new InstallmentPreviewResponse(currencyCode, registrationFee.add(courseFee).add(examinationFee), charges);
	}

	private LocalDate installmentDueDate(CourseBatch batch, FeePlanRequest request, int installmentNumber) {
		YearMonth month = YearMonth.from(batch.getStartDate()).plusMonths(installmentNumber - 1L);
		LocalDate dueDate = month.atDay(Math.min(request.monthlyDueDay(), month.lengthOfMonth()));
		if (installmentNumber == 1 && dueDate.isBefore(batch.getStartDate())) {
			return batch.getStartDate();
		}
		return dueDate;
	}

	private BatchResponse toResponse(CourseBatch batch) {
		long lecturerCount = batchLecturerRepository.countByBatchIdAndStatus(batch.getId(), BatchLecturerStatus.ACTIVE);
		boolean feePlanConfigured = feePlanRepository.findByBatchId(batch.getId()).isPresent();
		return batchMapper.toResponse(batch, lecturerCount, enrollmentRepository.countByBatchId(batch.getId()), feePlanConfigured);
	}

	private CourseBatch requireBatchManageAccess(Long batchId, Authentication authentication) {
		CurrentActor actor = adminActor(authentication);
		CourseBatch batch = findBatch(batchId);
		actor.requireBranchAccess(Set.of(batch.getBranch().getId()));
		return batch;
	}

	private CurrentActor adminActor(Authentication authentication) {
		CurrentActor actor = currentActorService.actor(authentication);
		actor.requireAnyRole("SUPER_ADMIN", "ADMIN");
		return actor;
	}

	private CourseBatch findBatch(Long id) {
		return batchRepository.findWithCourseAndBranchById(id).orElseThrow(() -> new NotFoundException("Batch was not found"));
	}

	private Course findCourse(Long id) {
		return courseRepository.findById(id).orElseThrow(() -> new NotFoundException("Course was not found"));
	}

	private Branch resolveAuthorizedBranch(Long branchId, CurrentActor actor) {
		Branch branch = branchRepository.findById(branchId).orElseThrow(() -> new NotFoundException("Branch was not found"));
		if (branch.getStatus() != BranchStatus.ACTIVE) {
			throw new IllegalArgumentException("Branch must be active");
		}
		actor.requireBranchAccess(Set.of(branchId));
		return branch;
	}

	private UserAccount resolveLecturer(Long lecturerUserId, Long branchId) {
		UserAccount lecturer = userRepository.findWithRolesAndBranchesById(lecturerUserId)
				.orElseThrow(() -> new IllegalArgumentException("Selected lecturer was not found"));
		if (lecturer.getStatus() != UserStatus.ACTIVE) {
			throw new IllegalArgumentException("Selected lecturer must be active");
		}
		boolean lecturerRole = lecturer.getRoles().stream().map(Role::getCode).anyMatch("LECTURER"::equals);
		if (!lecturerRole) {
			throw new IllegalArgumentException("Selected user must have the LECTURER role");
		}
		boolean branchAccess = lecturer.getBranches().stream().anyMatch(branch -> branch.getId().equals(branchId));
		if (!branchAccess) {
			throw new IllegalArgumentException("Selected lecturer must be assigned to the batch branch");
		}
		return lecturer;
	}

	private BatchLecturer findAssignment(Long batchId, Long assignmentId) {
		return batchLecturerRepository.findByIdAndBatchId(assignmentId, batchId)
				.orElseThrow(() -> new NotFoundException("Lecturer assignment was not found"));
	}

	private void validateBatchRequest(BatchRequest request) {
		if (request.startDate().isAfter(request.endDate())) {
			throw new IllegalArgumentException("Start date must not be after end date");
		}
	}

	private void validateFeePlan(FeePlanRequest request) {
		if (request.registrationFee() == null || request.courseFee() == null || request.examinationFee() == null) {
			throw new IllegalArgumentException("Fee amounts are required");
		}
		money(request.registrationFee());
		money(request.courseFee());
		money(request.examinationFee());
	}

	private void validateLecturerRequest(BatchLecturerRequest request) {
		if (request.assignmentEndDate() != null && request.assignmentEndDate().isBefore(request.assignmentStartDate())) {
			throw new IllegalArgumentException("Assignment end date must not be before the start date");
		}
	}

	private BigDecimal money(BigDecimal value) {
		if (value.signum() < 0) {
			throw new IllegalArgumentException("Fees must not be negative");
		}
		return value.setScale(2, RoundingMode.HALF_UP);
	}

	private String normalizeCurrency(String value) {
		String normalized = blankToNull(value);
		return normalized == null ? DEFAULT_CURRENCY : normalized.toUpperCase(Locale.ROOT);
	}

	private String normalizeRequired(String value) {
		return value.trim();
	}

	private String blankToNull(String value) {
		String normalized = value == null ? null : value.trim();
		return normalized == null || normalized.isBlank() ? null : normalized;
	}
}
