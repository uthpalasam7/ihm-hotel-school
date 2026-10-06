package com.ihm.hotelschool.session;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.ihm.hotelschool.auth.AuthTestData;
import com.ihm.hotelschool.user.UserStatus;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.*;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SessionLifecycleTests {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired JdbcTemplate db;
    @Autowired AuthTestData data;
    @Autowired Clock clock;
    @Autowired ClassSessionRepository sessions;
    long admin, lecturer, batch;

    @BeforeEach void setup() throws Exception {
        cleanup();
        admin = data.user("lifecycle_admin", "ADMIN", UserStatus.ACTIVE).getId();
        lecturer = data.user("lifecycle_lecturer", "LECTURER", UserStatus.ACTIVE).getId();
        long course = body(call(post("/api/v1/courses"), Map.of("name", "Manual course", "shortCode", "MAN", "status", "ACTIVE"), admin)
                .andExpect(status().isCreated())).path("id").asLong();
        batch = body(call(post("/api/v1/batches"), Map.of("courseId", course, "branchId", 1,
                "batchNumber", "MAN-01", "startDate", "2026-07-01", "endDate", "2026-07-31",
                "durationMonths", 1, "scheduleMode", "MANUAL", "status", "UPCOMING"), admin)
                .andExpect(status().isCreated())).path("id").asLong();
    }

    @AfterEach void cleanup() {
        for (var table : List.of("class_sessions", "batch_schedules", "document_deliveries", "student_card_events", "student_cards",
                "student_charges", "enrollments", "batch_lecturers", "fee_plans", "course_batches", "courses", "refresh_tokens",
                "audit_logs", "students", "user_roles", "user_branches", "users")) db.update("delete from " + table);
        db.update("delete from branches where id <> 1");
        db.update("update branches set status='ACTIVE' where id=1");
    }


    @Test void cancellationPreservesHistoryReasonAndBlocksAttendanceAndEditing() throws Exception {
        var saved = create(request().put("topic", "Safety"), admin);
        long id = saved.path("id").asLong();
        var cancelled = body(call(post(url(id, "cancel")), cancel().put("reason", "  Public holiday  "), admin).andExpect(status().isOk()));
        assertThat(cancelled.path("status").asText()).isEqualTo("CANCELLED");
        assertThat(cancelled.path("cancellationReason").asText()).isEqualTo("Public holiday");
        assertThat(cancelled.path("createdAt")).isEqualTo(saved.path("createdAt"));
        assertThat(cancelled.path("sessionDate")).isEqualTo(saved.path("sessionDate"));
        assertThat(cancelled.path("version").asLong()).isEqualTo(1);
        assertThat(count()).isEqualTo(1);
        assertThat(audits("SESSION_CANCELLED")).isEqualTo(1);
        assertThat(db.queryForObject("select reason from audit_logs where action='SESSION_CANCELLED'", String.class)).isEqualTo("Public holiday");
        assertThatThrownBy(() -> sessions.findById(id).orElseThrow().requireAttendanceEligible()).isInstanceOf(com.ihm.hotelschool.common.web.ConflictException.class);
        call(put("/api/v1/sessions/" + id), request().put("version", 1), admin).andExpect(status().isConflict());
        call(post(url(id, "cancel")), cancel().put("version", 1), admin).andExpect(status().isConflict());
        call(post(url(id, "reschedule")), move().put("version", 1), admin).andExpect(status().isConflict());
        create(request(), admin); // Cancellation releases the active slot without deleting history.
    }

    @Test void reschedulingCreatesLinkedReplacementAndAllowsChains() throws Exception {
        var saved = create(request().put("topic", "Safety").put("classroom", "Kitchen").put("remarks", "Bring apron"), admin);
        long id = saved.path("id").asLong();
        var result = move(id, move(), admin);
        var old = result.path("original"); var next = result.path("replacement");
        assertThat(old.path("status").asText()).isEqualTo("RESCHEDULED");
        assertThat(old.path("reschedulingReason").asText()).isEqualTo("Lecturer unavailable");
        assertThat(old.path("sessionDate")).isEqualTo(saved.path("sessionDate"));
        assertThat(next.path("status").asText()).isEqualTo("SCHEDULED");
        assertThat(next.path("originalSessionId").asLong()).isEqualTo(id);
        assertThat(next.path("sourceScheduleId").isNull()).isTrue();
        assertThat(next.path("generationDate").isNull()).isTrue();
        assertThat(next.path("topic").asText()).isEqualTo("Safety");
        assertThat(next.path("classroom").asText()).isEqualTo("Kitchen");
        assertThat(next.path("remarks").asText()).isEqualTo("Bring apron");
        assertThat(next.path("attendanceSubmittedAt").isNull()).isTrue();
        assertThatThrownBy(() -> sessions.findById(id).orElseThrow().requireAttendanceEligible()).isInstanceOf(com.ihm.hotelschool.common.web.ConflictException.class);
        sessions.findById(next.path("id").asLong()).orElseThrow().requireAttendanceEligible();
        var second = move(next.path("id").asLong(), move().put("newDate", "2026-07-31"), admin);
        assertThat(second.path("replacement").path("originalSessionId").asLong()).isEqualTo(next.path("id").asLong());
        assertThat(count()).isEqualTo(3);
        assertThat(audits("SESSION_RESCHEDULED")).isEqualTo(2);
        String evidence = db.queryForObject("select cast(new_value_json as varchar) from audit_logs where action='SESSION_RESCHEDULED' and entity_id=?", String.class, String.valueOf(id));
        assertThat(evidence).contains("original", "replacement");
        call(post(url(id, "reschedule")), move().put("version", 1), admin).andExpect(status().isConflict());
    }

    @Test void sameDayMoveCanReuseOriginalStartButMustActuallyChangeTime() throws Exception {
        long id = create(request(), admin).path("id").asLong();
        call(post(url(id, "reschedule")), move().put("newDate", "2026-07-01"), admin).andExpect(status().isBadRequest());
        var result = move(id, move().put("newDate", "2026-07-01").put("newEndTime", "14:00"), admin);
        assertThat(result.path("replacement").path("startTime").asText()).isEqualTo("09:00:00");
        assertThat(result.path("replacement").path("endTime").asText()).isEqualTo("14:00:00");
    }

    @ParameterizedTest @ValueSource(strings = {"EMPTY", "MISSING", "LONG", "NO_VERSION", "NEGATIVE_VERSION"})
    void cancellationAndReschedulingValidateReasonAndVersion(String kind) throws Exception {
        long id = create(request(), admin).path("id").asLong();
        for (var op : List.of("cancel", "reschedule")) {
            var body = op.equals("cancel") ? cancel() : move();
            switch (kind) {
                case "EMPTY" -> body.put("reason", "  ");
                case "MISSING" -> body.remove("reason");
                case "LONG" -> body.put("reason", "x".repeat(2001));
                case "NO_VERSION" -> body.remove("version");
                case "NEGATIVE_VERSION" -> body.put("version", -1);
            }
            call(post(url(id, op)), body, admin).andExpect(status().isBadRequest());
        }
        assertUnchanged(id);
    }

    @ParameterizedTest @ValueSource(strings = {"BEFORE", "AFTER", "REVERSED", "EQUAL", "NO_DATE", "NO_START", "NEGATIVE_TEACHER"})
    void invalidReplacementLeavesOriginalUntouched(String kind) throws Exception {
        long id = create(request(), admin).path("id").asLong();
        var body = move();
        switch (kind) {
            case "BEFORE" -> body.put("newDate", "2026-06-30");
            case "AFTER" -> body.put("newDate", "2026-08-01");
            case "REVERSED" -> body.put("newEndTime", "08:00");
            case "EQUAL" -> body.put("newEndTime", "09:00");
            case "NO_DATE" -> body.remove("newDate");
            case "NO_START" -> body.remove("newStartTime");
            case "NEGATIVE_TEACHER" -> body.put("lecturerUserId", -1);
        }
        call(post(url(id, "reschedule")), body, admin).andExpect(status().isBadRequest());
        assertUnchanged(id);
    }

    @ParameterizedTest @ValueSource(strings = {"COMPLETED", "CANCELLED", "RESCHEDULED", "ATTENDANCE"})
    void lifecycleProtectsTerminalAndSubmittedSessions(String state) throws Exception {
        long id = create(request(), admin).path("id").asLong();
        if (state.equals("ATTENDANCE")) db.update("update class_sessions set attendance_submitted_at=current_timestamp where id=?", id);
        else db.update("update class_sessions set status=?, cancellation_reason='History', rescheduling_reason='History' where id=?", state, id);
        call(post(url(id, "cancel")), cancel(), admin).andExpect(status().isConflict());
        call(post(url(id, "reschedule")), move(), admin).andExpect(status().isConflict());
        assertThat(count()).isEqualTo(1);
        assertThat(audits("SESSION_CANCELLED") + audits("SESSION_RESCHEDULED")).isZero();
    }

    @Test void staleRequestsCannotCancelOrMoveAnEditedSession() throws Exception {
        long id = create(request(), admin).path("id").asLong();
        call(put("/api/v1/sessions/" + id), request().put("version", 0).put("topic", "Updated"), admin).andExpect(status().isOk());
        call(post(url(id, "cancel")), cancel(), admin).andExpect(status().isConflict());
        call(post(url(id, "reschedule")), move(), admin).andExpect(status().isConflict());
        assertThat(count()).isEqualTo(1);
    }

    @Test void overlapsAreRejectedButAdjacentReplacementIsAllowed() throws Exception {
        long id = create(request(), admin).path("id").asLong();
        var occupied = create(request().put("sessionDate", "2026-07-02").put("startTime", "03:00").put("endTime", "05:00"), admin);
        var body = move().put("newStartTime", "04:00").put("newEndTime", "06:00");
        call(post(url(id, "reschedule")), body, admin).andExpect(status().isConflict());
        db.update("update class_sessions set status='COMPLETED' where id=?", occupied.path("id").asLong());
        call(post(url(id, "reschedule")), body, admin).andExpect(status().isConflict());
        move(id, body.put("newStartTime", "05:00"), admin);
    }

    @Test void namedLecturerIsValidatedForReplacementDate() throws Exception {
        long id = create(request(), admin).path("id").asLong();
        var body = move().put("lecturerUserId", lecturer);
        call(post(url(id, "reschedule")), body, admin).andExpect(status().isBadRequest());
        assign("2026-07-01", "2026-07-01");
        call(post(url(id, "reschedule")), body, admin).andExpect(status().isBadRequest());
        db.update("update batch_lecturers set assignment_end_date=date '2026-07-31'");
        db.update("update users set status='DISABLED' where id=?", lecturer);
        call(post(url(id, "reschedule")), body, admin).andExpect(status().isBadRequest());
        db.update("update users set status='ACTIVE' where id=?", lecturer);
        assertThat(move(id, body, admin).path("replacement").path("lecturerUserId").asLong()).isEqualTo(lecturer);
    }

    @Test void authorizationAppliesToBothActionsIncludingCurrentLecturerAssignments() throws Exception {
        long id = create(request(), admin).path("id").asLong();
        long other = data.branch("LIFE-OTHER").getId();
        long otherAdmin = data.user("life_other_admin", "ADMIN", UserStatus.ACTIVE, "LIFE-OTHER").getId();
        for (String op : List.of("cancel", "reschedule")) {
            var body = op.equals("cancel") ? cancel() : move();
            call(post(url(id, op)), body, lecturer).andExpect(status().isForbidden());
            call(post(url(id, op)), body, otherAdmin).andExpect(status().isForbidden());
            call(post(url(id, op)).header("X-Active-Branch-Id", other), body, admin).andExpect(status().isForbidden());
            mvc.perform(post(url(id, op)).contentType(MediaType.APPLICATION_JSON).content(body.toString())).andExpect(status().isUnauthorized());
            call(post(url(9999999, op)), body, admin).andExpect(status().isNotFound());
        }
        assign("2020-01-01", "2099-12-31");
        var next = move(id, move(), lecturer).path("replacement").path("id").asLong();
        String tomorrow = LocalDate.now(clock.withZone(ZoneId.of("Asia/Colombo"))).plusDays(1).toString();
        db.update("update batch_lecturers set assignment_start_date=cast(? as date)", tomorrow);
        call(post(url(next, "cancel")), cancel(), lecturer).andExpect(status().isForbidden());
        call(post(url(next, "reschedule")), move(), lecturer).andExpect(status().isForbidden());
        db.update("update batch_lecturers set assignment_start_date=date '2020-01-01'");
        call(post(url(next, "cancel")), cancel(), lecturer).andExpect(status().isOk());
        long superAdmin = data.user("life_super", "SUPER_ADMIN", UserStatus.ACTIVE, "LIFE-OTHER").getId();
        long third = create(request(), admin).path("id").asLong();
        call(post(url(third, "cancel")), cancel(), superAdmin).andExpect(status().isOk());
    }

    @ParameterizedTest @ValueSource(strings = {"COMPLETED", "CANCELLED", "INACTIVE_BRANCH"})
    void unavailableBatchesRejectLifecycle(String state) throws Exception {
        long id = create(request(), admin).path("id").asLong();
        if (state.equals("INACTIVE_BRANCH")) db.update("update branches set status='INACTIVE' where id=1");
        else db.update("update course_batches set status=? where id=?", state, batch);
        call(post(url(id, "cancel")), cancel(), admin).andExpect(status().isBadRequest());
        call(post(url(id, "reschedule")), move(), admin).andExpect(status().isBadRequest());
        assertUnchanged(id);
    }

    @Test void generatedOriginsSurviveLifecycleWithoutRecreatingOldDates() throws Exception {
        var generation = generationRequest();
        long id = body(call(post(generationUrl()), generation, admin).andExpect(status().isOk())).path("createdSessionIds").get(0).asLong();
        var result = move(id, move(), admin);
        assertThat(result.path("original").path("generationDate").asText()).isEqualTo("2026-07-01");
        assertThat(result.path("replacement").path("sourceScheduleId").isNull()).isTrue();
        long next = result.path("replacement").path("id").asLong();
        call(post(url(next, "cancel")), cancel(), admin).andExpect(status().isOk());
        call(post(generationUrl()), generation, admin).andExpect(status().isOk()).andExpect(jsonPath("$.createdCount").value(0));
        assertThat(count()).isEqualTo(2);
    }

    @Test void concurrentMovesCreateExactlyOneReplacement() throws Exception {
        long id = create(request(), admin).path("id").asLong();
        assertThat(race(() -> responseCode(post(url(id, "reschedule")), move()),
                () -> responseCode(post(url(id, "reschedule")), move().put("newDate", "2026-07-03"))))
                .containsExactlyInAnyOrder(201, 409);
        assertThat(count()).isEqualTo(2);
        assertThat(audits("SESSION_RESCHEDULED")).isEqualTo(1);
    }

    @Test void concurrentCancellationAndMoveCannotBothWin() throws Exception {
        long id = create(request(), admin).path("id").asLong();
        var results = race(() -> responseCode(post(url(id, "cancel")), cancel()),
                () -> responseCode(post(url(id, "reschedule")), move()));
        assertThat(results).contains(409);
        assertThat(results).anyMatch(code -> code == 200 || code == 201);
        assertThat(audits("SESSION_CANCELLED") + audits("SESSION_RESCHEDULED")).isEqualTo(1);
        assertThat(count()).isEqualTo(results.contains(201) ? 2 : 1);
    }

    @Test void manualCreateAndRescheduleShareOverlapLock() throws Exception {
        long id = create(request(), admin).path("id").asLong();
        assertThat(race(() -> responseCode(post(url(id, "reschedule")), move()),
                () -> responseCode(post("/api/v1/sessions"), request().put("sessionDate", "2026-07-02"))))
                .containsExactlyInAnyOrder(201, 409);
        assertThat(count()).isEqualTo(2);
    }

    @Test void auditFailureRollsBackOriginalReplacementAndCancellation() throws Exception {
        long id = create(request(), admin).path("id").asLong();
        db.execute("alter table audit_logs add constraint test_lifecycle_audit check (action not in ('SESSION_CANCELLED','SESSION_RESCHEDULED'))");
        try {
            assertThatThrownBy(() -> call(post(url(id, "cancel")), cancel(), admin)).hasRootCauseInstanceOf(java.sql.SQLException.class);
            assertUnchanged(id);
            assertThatThrownBy(() -> call(post(url(id, "reschedule")), move(), admin)).hasRootCauseInstanceOf(java.sql.SQLException.class);
            assertUnchanged(id);
        } finally { db.execute("alter table audit_logs drop constraint test_lifecycle_audit"); }
    }

    @Test void replacementInsertFailureRollsBackRetiredOriginal() throws Exception {
        long id = create(request(), admin).path("id").asLong();
        db.execute("alter table class_sessions add constraint test_replacement_failure check (original_session_id is null)");
        try {
            assertThatThrownBy(() -> call(post(url(id, "reschedule")), move(), admin)).hasRootCauseInstanceOf(java.sql.SQLException.class);
            assertUnchanged(id);
        } finally { db.execute("alter table class_sessions drop constraint test_replacement_failure"); }
    }

    @Test void databaseRequiresReasonsAndOnlyOneImmediateReplacement() throws Exception {
        long id = create(request(), admin).path("id").asLong();
        assertThatThrownBy(() -> db.update("update class_sessions set status='RESCHEDULED' where id=?", id))
                .isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
        assertThatThrownBy(() -> db.update("update class_sessions set status='CANCELLED' where id=?", id))
                .isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
        move(id, move(), admin);
        long extra = create(request().put("sessionDate", "2026-07-03"), admin).path("id").asLong();
        assertThatThrownBy(() -> db.update("update class_sessions set original_session_id=? where id=?", id, extra))
                .isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
    }

    private String url(long id, String action) { return "/api/v1/sessions/" + id + "/" + action; }
    private ObjectNode cancel() { return json.createObjectNode().put("reason", "Public holiday").put("version", 0); }
    private ObjectNode move() { return json.createObjectNode().put("newDate", "2026-07-02").put("newStartTime", "09:00")
            .put("newEndTime", "13:00").put("reason", "  Lecturer unavailable  ").put("version", 0); }
    private JsonNode move(long id, ObjectNode body, long actor) throws Exception {
        return body(call(post(url(id, "reschedule")), body, actor).andExpect(status().isCreated()));
    }
    private void assertUnchanged(long id) throws Exception {
        call(get("/api/v1/sessions/" + id), null, admin).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("SCHEDULED"))
                .andExpect(jsonPath("$.version").value(0)).andExpect(jsonPath("$.reschedulingReason").isEmpty());
        assertThat(count()).isEqualTo(1);
        assertThat(audits("SESSION_CANCELLED") + audits("SESSION_RESCHEDULED")).isZero();
    }

    private ObjectNode request() { return json.createObjectNode().put("batchId", batch).put("sessionDate", "2026-07-01").put("startTime", "09:00").put("endTime", "13:00"); }
    private JsonNode create(ObjectNode request, long actor) throws Exception { return body(call(post("/api/v1/sessions"), request, actor).andExpect(status().isCreated())); }
    private void assign(String from, String to) throws Exception {
        call(post("/api/v1/batches/" + batch + "/lecturers"), Map.of("lecturerUserId", lecturer, "assignmentStartDate", from, "assignmentEndDate", to, "status", "ACTIVE"), admin).andExpect(status().isCreated());
    }
    private String generationUrl() { return "/api/v1/batches/" + batch + "/sessions/generate"; }
    private ObjectNode generationRequest() throws Exception {
        db.update("update course_batches set schedule_mode='REGULAR' where id=?", batch);
        call(post("/api/v1/batches/" + batch + "/schedules"), Map.of("dayOfWeek", 3, "startTime", "09:00", "endTime", "13:00", "status", "ACTIVE"), admin).andExpect(status().isCreated());
        var range = json.createObjectNode().put("fromDate", "2026-07-01").put("toDate", "2026-07-01");
        var preview = body(call(post("/api/v1/batches/" + batch + "/sessions/preview"), range, admin).andExpect(status().isOk()));
        return range.put("previewToken", preview.path("previewToken").asText());
    }
    private List<Integer> race(Callable<Integer> first, Callable<Integer> second) throws Exception {
        var gate = new CountDownLatch(1);
        var pool = Executors.newFixedThreadPool(2);
        try {
            var a = pool.submit(() -> { gate.await(); return first.call(); });
            var b = pool.submit(() -> { gate.await(); return second.call(); });
            gate.countDown();
            return List.of(a.get(20, TimeUnit.SECONDS), b.get(20, TimeUnit.SECONDS));
        } finally { pool.shutdownNow(); }
    }
    private int responseCode(MockHttpServletRequestBuilder request, Object body) throws Exception { return call(request, body, admin).andReturn().getResponse().getStatus(); }
    private ResultActions call(MockHttpServletRequestBuilder request, Object body, long actor) throws Exception {
        request.with(jwt().jwt(j -> j.claim("userId", actor)));
        if (body != null) request.contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(body));
        return mvc.perform(request);
    }
    private JsonNode body(ResultActions result) throws Exception { return json.readTree(result.andReturn().getResponse().getContentAsString()); }
    private long count() { return db.queryForObject("select count(*) from class_sessions", Long.class); }
    private long audits(String action) { return db.queryForObject("select count(*) from audit_logs where action=?", Long.class, action); }
}
