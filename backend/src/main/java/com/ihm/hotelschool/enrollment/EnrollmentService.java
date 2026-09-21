package com.ihm.hotelschool.enrollment;

import com.ihm.hotelschool.audit.AuditService;
import com.ihm.hotelschool.batch.*;
import com.ihm.hotelschool.branch.BranchStatus;
import com.ihm.hotelschool.common.security.CurrentActorService;
import com.ihm.hotelschool.common.security.CurrentActorService.CurrentActor;
import com.ihm.hotelschool.common.web.ConflictException;
import com.ihm.hotelschool.common.web.NotFoundException;
import com.ihm.hotelschool.common.web.PageResponse;
import com.ihm.hotelschool.enrollment.dto.*;
import com.ihm.hotelschool.finance.StudentCharge;
import com.ihm.hotelschool.finance.StudentChargeRepository;
import com.ihm.hotelschool.student.Student;
import com.ihm.hotelschool.student.StudentRepository;
import com.ihm.hotelschool.student.StudentStatus;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Locale;
import java.util.Set;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EnrollmentService {
    private final EnrollmentRepository enrollments;
    private final CourseBatchRepository batches;
    private final StudentRepository students;
    private final FeePlanRepository fees;
    private final BatchLecturerRepository assignments;
    private final StudentChargeRepository charges;
    private final EnrollmentChargeCalculator calculator;
    private final CurrentActorService actors;
    private final AuditService audit;
    private final Clock clock;
    private final ZoneId zone;

    public EnrollmentService(EnrollmentRepository enrollments, CourseBatchRepository batches,
            StudentRepository students, FeePlanRepository fees, BatchLecturerRepository assignments, StudentChargeRepository charges,
            EnrollmentChargeCalculator calculator, CurrentActorService actors, AuditService audit,
            Clock clock, @Value("${app.timezone:Asia/Colombo}") String timezone) {
        this.enrollments = enrollments; this.batches = batches; this.students = students;
        this.fees = fees; this.assignments = assignments; this.charges = charges; this.calculator = calculator;
        this.actors = actors; this.audit = audit; this.clock = clock; this.zone = ZoneId.of(timezone);
    }

    @Transactional
    public EnrollmentResponse create(EnrollmentRequest request, org.springframework.security.core.Authentication auth,
            HttpServletRequest httpRequest) {
        CurrentActor actor = admin(auth);
        // This lock protects both duplicate checks and the per-batch sequence until commit.
        CourseBatch batch = batches.findForEnrollmentById(request.batchId())
                .orElseThrow(() -> new NotFoundException("Batch was not found"));
        authorize(batch, actor);
        Student student = students.findForEnrollmentById(request.studentId())
                .orElseThrow(() -> new NotFoundException("Student was not found"));
        validateEnrollment(request, batch, student);
        if (enrollments.existsByStudentIdAndBatchId(student.getId(), batch.getId())) {
            throw new ConflictException("Student is already enrolled in this batch");
        }
        FeePlan plan = fees.findForEnrollmentByBatchId(batch.getId())
                .orElseThrow(() -> new IllegalArgumentException("Configure an active fee plan before enrollment"));
        validateFees(batch, plan);
        if ((request.expectedBatchVersion() != null && !request.expectedBatchVersion().equals(batch.getVersion()))
                || (request.expectedFeePlanVersion() != null && !request.expectedFeePlanVersion().equals(plan.getVersion()))) {
            throw new ConflictException("Batch or fee plan changed. Preview the charges again before enrolling.");
        }
        var preview = calculator.calculate(plan, batch.getStartDate(), request.enrollmentDate());
        var now = clock.instant();
        int sequence = batch.nextRegistrationSequence(now, actor.id());
        Enrollment enrollment = enrollments.saveAndFlush(new Enrollment(student, batch, sequence,
                request.enrollmentDate(), trim(request.remarks()), plan, now, actor.id()));
        charges.saveAllAndFlush(preview.charges().stream()
                .map(charge -> new StudentCharge(enrollment, charge, now, actor.id())).toList());
        EnrollmentResponse response = response(enrollment);
        audit.record(actor.user(), batch.getBranch(), "ENROLLMENT_CREATED", "Enrollment", enrollment.getId(),
                null, response, null, httpRequest);
        return response;
    }

    @Transactional
    public EnrollmentResponse changeStatus(Long id, EnrollmentStatusRequest request,
            org.springframework.security.core.Authentication auth, HttpServletRequest httpRequest) {
        CurrentActor actor=admin(auth);
        Enrollment enrollment=accessible(id,actor);
        if (request.version()!=null && !request.version().equals(enrollment.getVersion()))
            throw new ConflictException("Enrollment changed. Reload it before updating the status.");
        EnrollmentResponse before=response(enrollment);
        enrollment.changeStatus(request.status(),clock.instant(),actor.id());
        if (before.status()==enrollment.getStatus()) return before;
        enrollments.flush();
        EnrollmentResponse after=response(enrollment);
        audit.record(actor.user(),enrollment.getBatch().getBranch(),"ENROLLMENT_STATUS_CHANGED","Enrollment",
                enrollment.getId(),before,after,request.reason().trim(),httpRequest);
        return after;
    }

    @Transactional(readOnly = true)
    public EnrollmentPreview preview(EnrollmentRequest request, org.springframework.security.core.Authentication auth) {
        CurrentActor actor = admin(auth);
        CourseBatch batch = batches.findWithCourseAndBranchById(request.batchId())
                .orElseThrow(() -> new NotFoundException("Batch was not found"));
        authorize(batch, actor);
        Student student = students.findById(request.studentId())
                .orElseThrow(() -> new NotFoundException("Student was not found"));
        validateEnrollment(request, batch, student);
        if (enrollments.existsByStudentIdAndBatchId(student.getId(), batch.getId())) {
            throw new ConflictException("Student is already enrolled in this batch");
        }
        FeePlan plan = fees.findByBatchId(batch.getId())
                .orElseThrow(() -> new IllegalArgumentException("Configure an active fee plan before enrollment"));
        validateFees(batch, plan);
        return calculator.calculate(plan, batch.getStartDate(), request.enrollmentDate());
    }

    @Transactional(readOnly = true)
    public PageResponse<EnrollmentResponse> list(Long branchId, Long batchId, Long studentId, String status,
            String search, Pageable pageable, org.springframework.security.core.Authentication auth) {
        CurrentActor actor = admin(auth);
        if (branchId != null) actor.requireBranchAccess(Set.of(branchId));
        Specification<Enrollment> spec = (root, query, cb) -> actor.superAdmin() ? cb.conjunction()
                : root.get("batch").get("branch").get("id").in(actor.branchIds());
        if (branchId != null) spec = spec.and((root, query, cb) -> cb.equal(root.get("batch").get("branch").get("id"), branchId));
        if (batchId != null) spec = spec.and((root, query, cb) -> cb.equal(root.get("batch").get("id"), batchId));
        if (studentId != null) spec = spec.and((root, query, cb) -> cb.equal(root.get("student").get("id"), studentId));
        if (status != null && !status.isBlank()) {
            EnrollmentStatus value = EnrollmentStatus.valueOf(status.toUpperCase(Locale.ROOT));
            spec = spec.and((root, query, cb) -> cb.equal(root.get("status"), value));
        }
        if (search != null && !search.isBlank()) {
            String pattern = "%" + search.trim().toLowerCase(Locale.ROOT) + "%";
            spec = spec.and((root, query, cb) -> cb.or(
                    cb.like(cb.lower(root.get("registrationNumber")), pattern),
                    cb.like(cb.lower(root.get("student").get("fullName")), pattern),
                    cb.like(cb.lower(root.get("batch").get("batchNumber")), pattern)));
        }
        return PageResponse.from(enrollments.findAll(spec, bounded(pageable, Sort.by("id").descending())), this::response);
    }

    @Transactional(readOnly = true)
    public EnrollmentResponse get(Long id, org.springframework.security.core.Authentication auth) {
        return response(accessible(id, admin(auth)));
    }

    @Transactional(readOnly = true)
    public PageResponse<BatchStudentResponse> roster(Long batchId, Pageable pageable,
            org.springframework.security.core.Authentication auth) {
        CurrentActor actor = actors.actor(auth);
        actor.requireAnyRole("SUPER_ADMIN", "ADMIN", "LECTURER");
        CourseBatch batch = batches.findWithCourseAndBranchById(batchId)
                .orElseThrow(() -> new NotFoundException("Batch was not found"));
        authorize(batch, actor);
        if (!actor.superAdmin() && !actor.admin() && !assignments.hasActiveAssignmentOn(
                batchId, actor.id(), LocalDate.now(clock.withZone(zone)))) {
            throw new AccessDeniedException("Access denied");
        }
        Specification<Enrollment> spec = (root, query, cb) -> cb.equal(root.get("batch").get("id"), batchId);
        return PageResponse.from(enrollments.findAll(spec, bounded(pageable,
                Sort.by("student.fullName").ascending().and(Sort.by("id")))),
                enrollment -> new BatchStudentResponse(enrollment.getStudent().getId(),
                        enrollment.getStudent().getFullName(), enrollment.getRegistrationNumber(),
                        enrollment.getEnrollmentDate(), enrollment.getStatus()));
    }

    @Transactional(readOnly = true)
    public PageResponse<StudentCharge.Response> charges(Long id, Pageable pageable,
            org.springframework.security.core.Authentication auth) {
        accessible(id, admin(auth));
        LocalDate today = LocalDate.now(clock.withZone(zone));
        return PageResponse.from(charges.findByEnrollmentId(id, bounded(pageable,
                Sort.by("dueDate").ascending().and(Sort.by("id")))), charge -> charge.toResponse(today));
    }

    private Enrollment accessible(Long id, CurrentActor actor) {
        Enrollment enrollment = enrollments.findWithStudentAndBatchById(id)
                .orElseThrow(() -> new NotFoundException("Enrollment was not found"));
        authorize(enrollment.getBatch(), actor);
        return enrollment;
    }

    private CurrentActor admin(org.springframework.security.core.Authentication auth) {
        CurrentActor actor = actors.actor(auth);
        actor.requireAnyRole("SUPER_ADMIN", "ADMIN");
        return actor;
    }

    private void authorize(CourseBatch batch, CurrentActor actor) {
        actor.requireBranchAccess(Set.of(batch.getBranch().getId()));
    }

    private void validateEnrollment(EnrollmentRequest request, CourseBatch batch, Student student) {
        if (student.getStatus() != StudentStatus.ACTIVE) throw new IllegalArgumentException("Only active students can enroll");
        if (batch.getStatus() != BatchStatus.ACTIVE && batch.getStatus() != BatchStatus.UPCOMING)
            throw new IllegalArgumentException("Only active or upcoming batches accept enrollments");
        if (batch.getBranch().getStatus() != BranchStatus.ACTIVE)
            throw new IllegalArgumentException("The batch branch is inactive");
        if (request.enrollmentDate().isAfter(batch.getEndDate()))
            throw new IllegalArgumentException("Enrollment date cannot be after the batch end date");
    }

    private void validateFees(CourseBatch batch, FeePlan plan) {
        if (plan.getStatus() != FeePlanStatus.ACTIVE) throw new IllegalArgumentException("The batch fee plan is inactive");
        if (!plan.getDurationMonths().equals(batch.getDurationMonths()))
            throw new IllegalArgumentException("Update the fee plan to match the batch duration before enrollment");
    }

    private Pageable bounded(Pageable pageable, Sort sort) {
        return PageRequest.of(pageable.getPageNumber(), Math.min(pageable.getPageSize(), 100), sort);
    }

    private String trim(String value) { return value == null || value.isBlank() ? null : value.trim(); }

    private EnrollmentResponse response(Enrollment e) {
        CourseBatch b = e.getBatch();
        return new EnrollmentResponse(e.getId(), e.getStudent().getId(), e.getStudent().getFullName(), b.getId(),
                b.getBatchNumber(), b.getCourse().getName(), b.getBranch().getId(), b.getBranch().getName(),
                e.getRegistrationNumber(), e.getEnrollmentDate(), e.getStatus(), e.getRemarks(), e.getCreatedAt(), e.getVersion());
    }
}
