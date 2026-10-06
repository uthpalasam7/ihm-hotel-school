package com.ihm.hotelschool.session;

import com.ihm.hotelschool.audit.AuditService;
import com.ihm.hotelschool.batch.*;
import com.ihm.hotelschool.common.security.CurrentActorService;
import com.ihm.hotelschool.common.security.CurrentActorService.CurrentActor;
import com.ihm.hotelschool.common.web.ConflictException;
import com.ihm.hotelschool.common.web.NotFoundException;
import com.ihm.hotelschool.session.dto.*;
import com.ihm.hotelschool.session.dto.GenerationPreview.Entry;
import com.ihm.hotelschool.session.dto.GenerationPreview.Outcome;
import com.ihm.hotelschool.user.UserAccount;
import com.ihm.hotelschool.user.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Clock;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SessionGenerationService {
    private static final int MAX_DAYS = 366;
    private static final int MAX_SESSIONS = 1000;
    private static final int MAX_EXISTING = 5000;
    private final CourseBatchRepository batches;
    private final ClassSessionRepository sessions;
    private final BatchLecturerRepository assignments;
    private final UserRepository users;
    private final ScheduleService schedules;
    private final GenerationPreviewSigner signer;
    private final CurrentActorService actors;
    private final AuditService audit;
    private final Clock clock;

    public SessionGenerationService(CourseBatchRepository batches, ClassSessionRepository sessions,
            BatchLecturerRepository assignments, UserRepository users, ScheduleService schedules,
            GenerationPreviewSigner signer, CurrentActorService actors, AuditService audit, Clock clock) {
        this.batches = batches; this.sessions = sessions; this.assignments = assignments; this.users = users;
        this.schedules = schedules; this.signer = signer; this.actors = actors; this.audit = audit; this.clock = clock;
    }

    @Transactional
    public GenerationPreview preview(Long batchId, GenerationRequest request, Authentication auth) {
        var actor = admin(auth);
        var plan = plan(batchId, request, actor);
        String token = signer.issue(plan.context());
        return new GenerationPreview(token, signer.expiresAt(token), count(plan.entries(), Outcome.CREATE),
                count(plan.entries(), Outcome.EXISTING) + count(plan.entries(), Outcome.ALREADY_GENERATED),
                count(plan.entries(), Outcome.CONFLICT), plan.entries());
    }

    @Transactional
    public GenerationResult generate(Long batchId, GenerationRequest request, Authentication auth, HttpServletRequest http) {
        var actor = admin(auth);
        var plan = plan(batchId, request, actor);
        signer.verify(request.previewToken(), plan.context());
        if (count(plan.entries(), Outcome.CONFLICT) > 0) {
            throw new ConflictException("Session generation has conflicts. Resolve them and preview again.");
        }
        Map<Long, BatchSchedule> byId = plan.patterns().stream().collect(Collectors.toMap(BatchSchedule::getId, Function.identity()));
        var created = plan.entries().stream().filter(e -> e.outcome() == Outcome.CREATE)
                .map(e -> ClassSession.generated(byId.get(e.scheduleId()), e.sessionDate(), clock.instant(), actor.id())).toList();
        var saved = sessions.saveAllAndFlush(created);
        var result = new GenerationResult(created.size(), plan.entries().size() - created.size(), saved.stream().map(ClassSession::getId).toList());
        if (!created.isEmpty()) {
            audit.record(actor.user(), plan.batch().getBranch(), "SESSIONS_GENERATED", "CourseBatch", batchId,
                    null, result, null, http);
        }
        return result;
    }

    private Plan plan(Long batchId, GenerationRequest request, CurrentActor actor) {
        if (batchId < 1) throw new IllegalArgumentException("Batch id must be positive");
        // All pattern changes and generation share this lock; it also serializes concurrent retries.
        var batch = batches.findForUpdateById(batchId).orElseThrow(() -> new NotFoundException("Batch was not found"));
        actor.requireBranchAccess(Set.of(batch.getBranch().getId()));
        ScheduleService.requireRegular(batch);
        long days = validateRange(batch, request);
        var patterns = schedules.active(batchId);
        if (patterns.isEmpty()) throw new IllegalArgumentException("Configure an active weekly pattern before generation");
        validatePatternOverlaps(patterns);
        var existing = sessions.findGenerationContext(batchId, request.fromDate(), request.toDate(), PageRequest.of(0, MAX_EXISTING + 1));
        if (existing.size() > MAX_EXISTING) throw new IllegalArgumentException("Too many existing sessions; preview a smaller date range");
        var teacherIds = patterns.stream().filter(p -> p.getDefaultLecturer() != null)
                .map(p -> p.getDefaultLecturer().getId()).collect(Collectors.toSet());
        Map<Long, UserAccount> teachers = teacherIds.isEmpty() ? Map.of() : users.findByIdIn(teacherIds).stream()
                .collect(Collectors.toMap(UserAccount::getId, Function.identity()));
        var activeAssignments = assignments.findByBatchIdAndStatus(batchId, BatchLecturerStatus.ACTIVE, PageRequest.of(0, 1001));
        if (activeAssignments.size() > 1000) throw new IllegalArgumentException("Too many active lecturer assignments for generation");
        var exclusions = new TreeSet<>(request.excludeDates());
        var entries = new ArrayList<Entry>();
        for (int offset = 0; offset <= days; offset++) {
            LocalDate date = request.fromDate().plusDays(offset);
            if (exclusions.contains(date)) continue;
            for (var pattern : patterns) {
                if (pattern.getDayOfWeek() != date.getDayOfWeek().getValue()) continue;
                if (entries.size() == MAX_SESSIONS) throw new IllegalArgumentException("Preview exceeds 1000 sessions; use a smaller date range");
                entries.add(classify(batch, pattern, date, existing, teachers, activeAssignments));
            }
        }
        var context = new PreviewContext(actor.id(), batchId, batch.getVersion(), request.fromDate(), request.toDate(),
                List.copyOf(exclusions), patterns.stream().map(ScheduleService::response).toList());
        return new Plan(batch, patterns, List.copyOf(entries), context);
    }

    private Entry classify(CourseBatch batch, BatchSchedule pattern, LocalDate date, List<ClassSession> existing,
            Map<Long, UserAccount> teachers, List<BatchLecturer> activeAssignments) {
        Long lecturerId = pattern.getDefaultLecturer() == null ? null : pattern.getDefaultLecturer().getId();
        for (var session : existing) {
            if (session.getSourceSchedule() != null && session.getSourceSchedule().getId().equals(pattern.getId())
                    && date.equals(session.getGenerationDate())) {
                return entry(pattern, date, Outcome.ALREADY_GENERATED, session.getId(), "Previously generated; its current details and status are retained");
            }
        }
        for (var session : existing) {
            if (!session.getSessionDate().equals(date)) continue;
            Long existingTeacher = session.getLecturer() == null ? null : session.getLecturer().getId();
            boolean sameSlot = session.getStartTime().equals(pattern.getStartTime()) && Objects.equals(lecturerId, existingTeacher);
            boolean retired = session.getStatus() == SessionStatus.CANCELLED || session.getStatus() == SessionStatus.RESCHEDULED;
            if (sameSlot && retired) return entry(pattern, date, Outcome.EXISTING, session.getId(), "Cancelled or rescheduled session retained");
            if (sameSlot && session.getEndTime().equals(pattern.getEndTime()) && Objects.equals(session.getClassroom(), pattern.getClassroom())) {
                return entry(pattern, date, Outcome.EXISTING, session.getId(), "Matching session already exists");
            }
            if (!retired && session.getStartTime().isBefore(pattern.getEndTime()) && pattern.getStartTime().isBefore(session.getEndTime())) {
                return entry(pattern, date, Outcome.CONFLICT, session.getId(), "Overlaps an existing session in this batch");
            }
        }
        if (lecturerId != null && (!ScheduleService.eligibleLecturer(teachers.get(lecturerId), batch.getBranch().getId())
                || activeAssignments.stream().noneMatch(a -> a.getLecturer().getId().equals(lecturerId)
                    && !a.getAssignmentStartDate().isAfter(date)
                    && (a.getAssignmentEndDate() == null || !a.getAssignmentEndDate().isBefore(date))))) {
            return entry(pattern, date, Outcome.CONFLICT, null, "Lecturer needs active branch and batch assignment on this session date");
        }
        return entry(pattern, date, Outcome.CREATE, null, null);
    }

    private Entry entry(BatchSchedule pattern, LocalDate date, Outcome outcome, Long id, String message) {
        var teacher = pattern.getDefaultLecturer();
        return new Entry(pattern.getId(), date, pattern.getStartTime(), pattern.getEndTime(), teacher == null ? null : teacher.getId(),
                teacher == null ? null : teacher.getFullName(), pattern.getClassroom(), outcome, id, message);
    }

    private long validateRange(CourseBatch batch, GenerationRequest request) {
        if (request.fromDate() == null || request.toDate() == null || request.fromDate().isAfter(request.toDate())) {
            throw new IllegalArgumentException("A valid inclusive generation date range is required");
        }
        long days = ChronoUnit.DAYS.between(request.fromDate(), request.toDate());
        if (days >= MAX_DAYS) throw new IllegalArgumentException("Generate at most 366 days at a time");
        SessionValidation.date(batch, request.fromDate());
        SessionValidation.date(batch, request.toDate());
        if (request.excludeDates().size() > MAX_DAYS || request.excludeDates().stream().anyMatch(d -> d == null
                || d.isBefore(request.fromDate()) || d.isAfter(request.toDate()))) {
            throw new IllegalArgumentException("Excluded dates must be within the generation range");
        }
        return days;
    }

    private void validatePatternOverlaps(List<BatchSchedule> patterns) {
        for (int i = 0; i < patterns.size(); i++) {
            var a = patterns.get(i);
            for (int j = i + 1; j < patterns.size(); j++) {
                var b = patterns.get(j);
                if (a.getDayOfWeek().equals(b.getDayOfWeek()) && a.getStartTime().isBefore(b.getEndTime()) && b.getStartTime().isBefore(a.getEndTime())) {
                    throw new ConflictException("Active weekly patterns overlap; correct them before generation");
                }
            }
        }
    }
    private int count(List<Entry> entries, Outcome outcome) { return (int) entries.stream().filter(e -> e.outcome() == outcome).count(); }
    private CurrentActor admin(Authentication auth) {
        var actor = actors.actor(auth); actor.requireAnyRole("SUPER_ADMIN", "ADMIN"); return actor;
    }
    private record PreviewContext(Long actorId, Long batchId, Long batchVersion, LocalDate fromDate,
            LocalDate toDate, List<LocalDate> exclusions, List<ScheduleResponse> patterns) {}
    private record Plan(CourseBatch batch, List<BatchSchedule> patterns, List<Entry> entries, PreviewContext context) {}
}
