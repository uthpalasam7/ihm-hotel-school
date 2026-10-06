package com.ihm.hotelschool.session;

import com.ihm.hotelschool.audit.AuditService;
import com.ihm.hotelschool.batch.*;
import com.ihm.hotelschool.branch.BranchStatus;
import com.ihm.hotelschool.common.security.CurrentActorService;
import com.ihm.hotelschool.common.security.CurrentActorService.CurrentActor;
import com.ihm.hotelschool.common.web.*;
import com.ihm.hotelschool.session.dto.*;
import com.ihm.hotelschool.user.*;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Set;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class SessionWriteService {
    private final ClassSessionRepository sessions;
    private final SessionOverlapRepository overlaps;
    private final CourseBatchRepository batches;
    private final BatchLecturerRepository assignments;
    private final UserRepository users;
    private final CurrentActorService actors;
    private final AuditService audit;
    private final Clock clock;
    private final ZoneId zone;

    public SessionWriteService(ClassSessionRepository sessions, SessionOverlapRepository overlaps, CourseBatchRepository batches,
            BatchLecturerRepository assignments, UserRepository users, CurrentActorService actors,
            AuditService audit, Clock clock, @Value("${app.timezone:Asia/Colombo}") String timezone) {
        this.sessions = sessions; this.overlaps = overlaps; this.batches = batches; this.assignments = assignments;
        this.users = users; this.actors = actors; this.audit = audit; this.clock = clock; this.zone = ZoneId.of(timezone);
    }

    public SessionResponse create(SessionRequest request, Authentication auth, HttpServletRequest http) {
        var actor = writer(auth);
        var batch = writable(request.batchId(), actor);
        validateSlot(batch, null, request);
        var session = sessions.saveAndFlush(new ClassSession(batch, request.sessionDate(), request.startTime(),
                request.endTime(), lecturer(batch, request.lecturerUserId(), request.sessionDate()), request.topic(),
                request.classroom(), request.remarks(), clock.instant(), actor.id()));
        var response = SessionService.response(session);
        audit.record(actor.user(), batch.getBranch(), "SESSION_CREATED", "ClassSession", session.getId(), null, response, null, http);
        return response;
    }

    public SessionResponse update(Long id, SessionRequest request, Authentication auth, HttpServletRequest http) {
        var actor = writer(auth);
        var session = lockedSession(id, actor);
        var batch = session.getBatch();
        Long batchId = batch.getId();
        if (!batchId.equals(request.batchId())) throw new IllegalArgumentException("A session cannot be moved to another batch");
        requireVersion(session, request.version());
        if (session.getStatus() != SessionStatus.SCHEDULED || session.getAttendanceSubmittedAt() != null) {
            throw new ConflictException("Only scheduled sessions without submitted attendance can be edited");
        }
        validateSlot(batch, id, request);
        var before = SessionService.response(session);
        session.update(request.sessionDate(), request.startTime(), request.endTime(),
                lecturer(batch, request.lecturerUserId(), request.sessionDate()), request.topic(), request.classroom(),
                request.remarks(), clock.instant(), actor.id());
        sessions.flush();
        var response = SessionService.response(session);
        audit.record(actor.user(), batch.getBranch(), "SESSION_UPDATED", "ClassSession", id, before, response, null, http);
        return response;
    }

    public SessionResponse cancel(Long id, SessionCancellationRequest request, Authentication auth, HttpServletRequest http) {
        var actor = writer(auth);
        var session = lockedSession(id, actor);
        requireVersion(session, request.version());
        var before = SessionService.response(session);
        session.cancel(request.reason(), clock.instant(), actor.id());
        sessions.flush();
        var response = SessionService.response(session);
        audit.record(actor.user(), session.getBatch().getBranch(), "SESSION_CANCELLED", "ClassSession", id,
                before, response, session.getCancellationReason(), http);
        return response;
    }

    public SessionRescheduleResponse reschedule(Long id, SessionRescheduleRequest request, Authentication auth, HttpServletRequest http) {
        var actor = writer(auth);
        var original = lockedSession(id, actor);
        requireVersion(original, request.version());
        original.requireLifecycleChange();
        var batch = original.getBatch();
        SessionValidation.date(batch, request.newDate());
        SessionValidation.times(request.newStartTime(), request.newEndTime());
        if (overlaps.exists(batch.getId(), request.newDate(), request.newStartTime(), request.newEndTime(), id)) {
            throw new ConflictException("Replacement overlaps a scheduled or completed session in this batch");
        }
        var teacher = lecturer(batch, request.lecturerUserId(), request.newDate());
        var before = SessionService.response(original);
        var replacement = original.reschedule(request.newDate(), request.newStartTime(), request.newEndTime(), teacher,
                request.reason(), clock.instant(), actor.id());
        // Release the original active slot before inserting, including same-day time changes.
        sessions.flush();
        replacement = sessions.saveAndFlush(replacement);
        var response = new SessionRescheduleResponse(SessionService.response(original), SessionService.response(replacement));
        audit.record(actor.user(), batch.getBranch(), "SESSION_RESCHEDULED", "ClassSession", id,
                before, response, original.getReschedulingReason(), http);
        return response;
    }

    private ClassSession lockedSession(Long id, CurrentActor actor) {
        if (id < 1) throw new IllegalArgumentException("Session id must be positive");
        // Resolve only the immutable batch id before locking to avoid caching a stale session version.
        Long batchId = sessions.findBatchIdBySessionId(id).orElseThrow(() -> new NotFoundException("Session was not found"));
        writable(batchId, actor);
        return sessions.findWithBatchById(id).orElseThrow(() -> new NotFoundException("Session was not found"));
    }

    private void requireVersion(ClassSession session, Long version) {
        if (version == null || !version.equals(session.getVersion())) {
            throw new ConflictException("Session changed. Reload it before updating.");
        }
    }

    private CurrentActor writer(Authentication auth) {
        var actor = actors.actor(auth);
        actor.requireAnyRole("SUPER_ADMIN", "ADMIN", "LECTURER");
        return actor;
    }

    private CourseBatch writable(Long id, CurrentActor actor) {
        // All schedule/session writers share this lock, including bulk generation and batch edits.
        var batch = batches.findForUpdateById(id).orElseThrow(() -> new NotFoundException("Batch was not found"));
        actor.requireBranchAccess(Set.of(batch.getBranch().getId()));
        if (!actor.admin() && !actor.superAdmin()
                && !assignments.hasActiveAssignmentOn(id, actor.id(), LocalDate.now(clock.withZone(zone)))) {
            throw new AccessDeniedException("Access denied");
        }
        if (batch.getBranch().getStatus() != BranchStatus.ACTIVE
                || (batch.getStatus() != BatchStatus.ACTIVE && batch.getStatus() != BatchStatus.UPCOMING)) {
            throw new IllegalArgumentException("An active branch and active/upcoming batch are required");
        }
        return batch;
    }

    private void validateSlot(CourseBatch batch, Long id, SessionRequest request) {
        SessionValidation.date(batch, request.sessionDate());
        SessionValidation.times(request.startTime(), request.endTime());
        if (overlaps.exists(batch.getId(), request.sessionDate(), request.startTime(), request.endTime(), id)) {
            throw new ConflictException("Session overlaps a scheduled or completed session in this batch");
        }
    }

    private UserAccount lecturer(CourseBatch batch, Long id, LocalDate date) {
        if (id == null) return null;
        var user = users.findWithRolesAndBranchesById(id).orElseThrow(() -> new NotFoundException("Lecturer was not found"));
        if (!ScheduleService.eligibleLecturer(user, batch.getBranch().getId())
                || !assignments.hasActiveAssignmentOn(batch.getId(), id, date)) {
            throw new IllegalArgumentException("Lecturer must be active in the branch and assigned to the batch on the session date");
        }
        return user;
    }
}
