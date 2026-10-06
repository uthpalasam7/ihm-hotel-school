package com.ihm.hotelschool.session;

import com.ihm.hotelschool.batch.BatchLecturer;
import com.ihm.hotelschool.batch.BatchLecturerRepository;
import com.ihm.hotelschool.batch.BatchLecturerStatus;
import com.ihm.hotelschool.batch.CourseBatch;
import com.ihm.hotelschool.batch.CourseBatchRepository;
import com.ihm.hotelschool.common.security.CurrentActorService;
import com.ihm.hotelschool.common.security.CurrentActorService.CurrentActor;
import com.ihm.hotelschool.common.web.NotFoundException;
import com.ihm.hotelschool.common.web.PageResponse;
import com.ihm.hotelschool.session.dto.SessionResponse;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Locale;
import java.util.Set;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class SessionService {
    private final ClassSessionRepository sessions;
    private final CourseBatchRepository batches;
    private final BatchLecturerRepository assignments;
    private final CurrentActorService actors;
    private final Clock clock;
    private final ZoneId zone;

    public SessionService(ClassSessionRepository sessions, CourseBatchRepository batches,
            BatchLecturerRepository assignments, CurrentActorService actors, Clock clock,
            @Value("${app.timezone:Asia/Colombo}") String timezone) {
        this.sessions = sessions;
        this.batches = batches;
        this.assignments = assignments;
        this.actors = actors;
        this.clock = clock;
        this.zone = ZoneId.of(timezone);
    }

    public PageResponse<SessionResponse> list(Long branchId, Long batchId, Long lecturerId,
            LocalDate dateFrom, LocalDate dateTo, String status, int page, int size, Authentication auth) {
        CurrentActor actor = reader(auth);
        positive(branchId, "Branch id");
        positive(batchId, "Batch id");
        positive(lecturerId, "Lecturer id");
        if (page < 0 || size < 1) throw new IllegalArgumentException("Page must be non-negative and size must be positive");
        if (dateFrom != null && dateTo != null && dateFrom.isAfter(dateTo)) {
            throw new IllegalArgumentException("Date from must not be after date to");
        }
        SessionStatus state = status(status);
        LocalDate today = LocalDate.now(clock.withZone(zone));
        if (branchId != null) actor.requireBranchAccess(Set.of(branchId));
        if (batchId != null) authorize(batch(batchId), actor, today);

        Specification<ClassSession> spec = (root, query, cb) -> actor.superAdmin() ? cb.conjunction()
                : root.get("batch").get("branch").get("id").in(actor.branchIds());
        // An EXISTS subquery scopes both the rows and the pagination count without multiplying rows.
        if (!actor.superAdmin() && !actor.admin()) {
            spec = spec.and((root, query, cb) -> {
                var subquery = query.subquery(Long.class);
                var assignment = subquery.from(BatchLecturer.class);
                subquery.select(assignment.get("id")).where(
                        cb.equal(assignment.get("batch").get("id"), root.get("batch").get("id")),
                        cb.equal(assignment.get("lecturer").get("id"), actor.id()),
                        cb.equal(assignment.get("status"), BatchLecturerStatus.ACTIVE),
                        cb.lessThanOrEqualTo(assignment.get("assignmentStartDate"), today),
                        cb.or(cb.isNull(assignment.get("assignmentEndDate")),
                                cb.greaterThanOrEqualTo(assignment.get("assignmentEndDate"), today)));
                return cb.exists(subquery);
            });
        }
        if (branchId != null) spec = spec.and((root, query, cb) -> cb.equal(root.get("batch").get("branch").get("id"), branchId));
        if (batchId != null) spec = spec.and((root, query, cb) -> cb.equal(root.get("batch").get("id"), batchId));
        if (lecturerId != null) spec = spec.and((root, query, cb) -> cb.equal(root.get("lecturer").get("id"), lecturerId));
        if (dateFrom != null) spec = spec.and((root, query, cb) -> cb.greaterThanOrEqualTo(root.get("sessionDate"), dateFrom));
        if (dateTo != null) spec = spec.and((root, query, cb) -> cb.lessThanOrEqualTo(root.get("sessionDate"), dateTo));
        if (state != null) spec = spec.and((root, query, cb) -> cb.equal(root.get("status"), state));
        var pageable = PageRequest.of(page, Math.min(size, 100), Sort.by("sessionDate", "startTime", "id"));
        return PageResponse.from(sessions.findAll(spec, pageable), SessionService::response);
    }

    public SessionResponse get(Long id, Authentication auth) {
        CurrentActor actor = reader(auth);
        positive(id, "Session id");
        ClassSession session = sessions.findWithBatchById(id)
                .orElseThrow(() -> new NotFoundException("Session was not found"));
        authorize(session.getBatch(), actor, LocalDate.now(clock.withZone(zone)));
        return response(session);
    }

    private CurrentActor reader(Authentication auth) {
        CurrentActor actor = actors.actor(auth);
        actor.requireAnyRole("SUPER_ADMIN", "ADMIN", "LECTURER");
        return actor;
    }

    private CourseBatch batch(Long id) {
        return batches.findWithCourseAndBranchById(id).orElseThrow(() -> new NotFoundException("Batch was not found"));
    }

    private void authorize(CourseBatch batch, CurrentActor actor, LocalDate today) {
        actor.requireBranchAccess(Set.of(batch.getBranch().getId()));
        if (!actor.superAdmin() && !actor.admin() && !assignments.hasActiveAssignmentOn(batch.getId(), actor.id(), today)) {
            throw new AccessDeniedException("Access denied");
        }
    }

    private void positive(Long id, String field) {
        if (id != null && id < 1) throw new IllegalArgumentException(field + " must be positive");
    }

    private SessionStatus status(String value) {
        if (value == null || value.isBlank()) return null;
        try { return SessionStatus.valueOf(value.trim().toUpperCase(Locale.ROOT)); }
        catch (IllegalArgumentException exception) { throw new IllegalArgumentException("Session status is invalid"); }
    }

    static SessionResponse response(ClassSession session) {
        var batch = session.getBatch();
        var lecturer = session.getLecturer();
        return new SessionResponse(session.getId(), batch.getId(), batch.getBatchNumber(), batch.getCourse().getName(),
                batch.getBranch().getId(), batch.getBranch().getName(), session.getSessionDate(), session.getStartTime(),
                session.getEndTime(), lecturer == null ? null : lecturer.getId(), lecturer == null ? null : lecturer.getFullName(),
                session.getTopic(), session.getClassroom(), session.getStatus(), session.getCancellationReason(),
                session.getOriginalSession() == null ? null : session.getOriginalSession().getId(), session.getRemarks(),
                session.getAttendanceSubmittedAt(), session.getCreatedAt(), session.getUpdatedAt(), session.getVersion(),
                session.getSourceSchedule() == null ? null : session.getSourceSchedule().getId(), session.getGenerationDate(), session.getReschedulingReason());
    }
}
