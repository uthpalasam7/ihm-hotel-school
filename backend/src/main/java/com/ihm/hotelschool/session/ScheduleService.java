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
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ScheduleService {
    static final int MAX_PATTERNS = 100;
    private final CourseBatchRepository batches;
    private final BatchScheduleRepository schedules;
    private final BatchLecturerRepository assignments;
    private final UserRepository users;
    private final CurrentActorService actors;
    private final AuditService audit;
    private final Clock clock;
    private final ZoneId zone;

    public ScheduleService(CourseBatchRepository batches, BatchScheduleRepository schedules,
            BatchLecturerRepository assignments, UserRepository users, CurrentActorService actors,
            AuditService audit, Clock clock, @Value("${app.timezone:Asia/Colombo}") String timezone) {
        this.batches = batches; this.schedules = schedules; this.assignments = assignments;
        this.users = users; this.actors = actors; this.audit = audit; this.clock = clock; this.zone = ZoneId.of(timezone);
    }

    @Transactional(readOnly = true)
    public PageResponse<ScheduleResponse> list(Long batchId, int page, int size, Authentication auth) {
        CurrentActor actor = actors.actor(auth);
        actor.requireAnyRole("SUPER_ADMIN", "ADMIN", "LECTURER");
        CourseBatch batch = findBatch(batchId, false);
        actor.requireBranchAccess(Set.of(batch.getBranch().getId()));
        if (!actor.admin() && !actor.superAdmin() && !assignments.hasActiveAssignmentOn(batchId, actor.id(), LocalDate.now(clock.withZone(zone)))) {
            throw new AccessDeniedException("Access denied");
        }
        if (page < 0 || size < 1) throw new IllegalArgumentException("Page must be non-negative and size must be positive");
        return PageResponse.from(schedules.findByBatchId(batchId, PageRequest.of(page, Math.min(size, 100),
                Sort.by("dayOfWeek", "startTime", "id"))), ScheduleService::response);
    }

    @Transactional
    public ScheduleResponse create(Long batchId, ScheduleRequest request, Authentication auth, HttpServletRequest http) {
        CurrentActor actor = admin(auth);
        CourseBatch batch = writable(batchId, actor);
        validatePattern(batch, null, request);
        UserAccount lecturer = lecturer(batch, request);
        var schedule = new BatchSchedule(batch, request.dayOfWeek(), request.startTime(), request.endTime(), lecturer,
                request.classroom(), clock.instant(), actor.id());
        if (request.status() == ScheduleStatus.INACTIVE) schedule.deactivate(clock.instant(), actor.id());
        schedule = schedules.saveAndFlush(schedule);
        var response = response(schedule);
        audit.record(actor.user(), batch.getBranch(), "BATCH_SCHEDULE_CREATED", "BatchSchedule", schedule.getId(), null, response, null, http);
        return response;
    }

    @Transactional
    public ScheduleResponse update(Long batchId, Long id, ScheduleRequest request, Authentication auth, HttpServletRequest http) {
        CurrentActor actor = admin(auth);
        CourseBatch batch = writable(batchId, actor);
        var schedule = findSchedule(batchId, id);
        version(request.version(), schedule);
        validatePattern(batch, id, request);
        var before = response(schedule);
        schedule.update(request.dayOfWeek(), request.startTime(), request.endTime(), lecturer(batch, request),
                request.classroom(), request.status(), clock.instant(), actor.id());
        schedules.flush();
        var response = response(schedule);
        audit.record(actor.user(), batch.getBranch(), "BATCH_SCHEDULE_UPDATED", "BatchSchedule", id, before, response, null, http);
        return response;
    }

    @Transactional
    public void deactivate(Long batchId, Long id, Long version, Authentication auth, HttpServletRequest http) {
        CurrentActor actor = admin(auth);
        CourseBatch batch = findBatch(batchId, true);
        actor.requireBranchAccess(Set.of(batch.getBranch().getId()));
        var schedule = findSchedule(batchId, id);
        version(version, schedule);
        if (schedule.getStatus() == ScheduleStatus.INACTIVE) return;
        var before = response(schedule);
        schedule.deactivate(clock.instant(), actor.id());
        schedules.flush();
        audit.record(actor.user(), batch.getBranch(), "BATCH_SCHEDULE_DEACTIVATED", "BatchSchedule", id, before, response(schedule), null, http);
    }

    private void validatePattern(CourseBatch batch, Long id, ScheduleRequest request) {
        SessionValidation.times(request.startTime(), request.endTime());
        if (request.status() != ScheduleStatus.ACTIVE) return;
        List<BatchSchedule> active = active(batch.getId());
        if (active.size() >= MAX_PATTERNS && active.stream().noneMatch(s -> Objects.equals(s.getId(), id))) {
            throw new IllegalArgumentException("A batch supports at most 100 active weekly patterns");
        }
        if (active.stream().anyMatch(s -> !Objects.equals(s.getId(), id) && s.getDayOfWeek().equals(request.dayOfWeek())
                && request.startTime().isBefore(s.getEndTime()) && s.getStartTime().isBefore(request.endTime()))) {
            throw new ConflictException("Weekly pattern overlaps an active pattern in this batch");
        }
    }

    List<BatchSchedule> active(Long batchId) {
        var active = schedules.findByBatchIdAndStatusOrderByDayOfWeekAscStartTimeAscIdAsc(batchId,
                ScheduleStatus.ACTIVE, PageRequest.of(0, MAX_PATTERNS + 1));
        if (active.size() > MAX_PATTERNS) throw new IllegalArgumentException("Too many active weekly patterns");
        return active;
    }

    private UserAccount lecturer(CourseBatch batch, ScheduleRequest request) {
        if (request.defaultLecturerUserId() == null) return null;
        var user = users.findWithRolesAndBranchesById(request.defaultLecturerUserId())
                .orElseThrow(() -> new NotFoundException("Lecturer was not found"));
        if (request.status() == ScheduleStatus.ACTIVE) {
            if (!eligibleLecturer(user, batch.getBranch().getId())) throw new IllegalArgumentException("Lecturer must be active and assigned to the batch branch");
            var assignment = assignments.findByBatchIdAndLecturerIdAndStatus(batch.getId(), user.getId(), BatchLecturerStatus.ACTIVE)
                    .orElseThrow(() -> new IllegalArgumentException("Lecturer must have an active batch assignment"));
            if (assignment.getAssignmentStartDate().isAfter(batch.getEndDate()) ||
                    (assignment.getAssignmentEndDate() != null && assignment.getAssignmentEndDate().isBefore(batch.getStartDate()))) {
                throw new IllegalArgumentException("Lecturer assignment must overlap the batch dates");
            }
        }
        return user;
    }

    static boolean eligibleLecturer(UserAccount user, Long branchId) {
        return user != null && user.getStatus() == UserStatus.ACTIVE
                && user.getRoles().stream().anyMatch(r -> r.getCode().equals("LECTURER"))
                && user.getBranches().stream().anyMatch(b -> b.getId().equals(branchId));
    }

    private CurrentActor admin(Authentication auth) {
        var actor = actors.actor(auth); actor.requireAnyRole("SUPER_ADMIN", "ADMIN"); return actor;
    }
    private CourseBatch writable(Long id, CurrentActor actor) {
        var batch = findBatch(id, true); actor.requireBranchAccess(Set.of(batch.getBranch().getId()));
        requireRegular(batch); return batch;
    }
    static void requireRegular(CourseBatch batch) {
        if (batch.getScheduleMode() != ScheduleMode.REGULAR || batch.getBranch().getStatus() != BranchStatus.ACTIVE
                || (batch.getStatus() != BatchStatus.ACTIVE && batch.getStatus() != BatchStatus.UPCOMING)) {
            throw new IllegalArgumentException("An active branch and active/upcoming regular batch are required");
        }
    }
    private CourseBatch findBatch(Long id, boolean lock) {
        if (id < 1) throw new IllegalArgumentException("Batch id must be positive");
        return (lock ? batches.findForUpdateById(id) : batches.findWithCourseAndBranchById(id))
                .orElseThrow(() -> new NotFoundException("Batch was not found"));
    }
    private BatchSchedule findSchedule(Long batchId, Long id) {
        return schedules.findByIdAndBatchId(id, batchId).orElseThrow(() -> new NotFoundException("Weekly pattern was not found"));
    }
    private void version(Long version, BatchSchedule schedule) {
        if (version == null || !version.equals(schedule.getVersion())) throw new ConflictException("Weekly pattern changed. Reload it before updating.");
    }
    static ScheduleResponse response(BatchSchedule schedule) {
        var teacher = schedule.getDefaultLecturer();
        return new ScheduleResponse(schedule.getId(), schedule.getBatch().getId(), schedule.getDayOfWeek(),
                schedule.getStartTime(), schedule.getEndTime(), teacher == null ? null : teacher.getId(),
                teacher == null ? null : teacher.getFullName(), schedule.getClassroom(), schedule.getStatus(), schedule.getVersion());
    }
}
