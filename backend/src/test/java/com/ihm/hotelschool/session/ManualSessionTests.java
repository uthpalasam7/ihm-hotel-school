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
class ManualSessionTests {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired JdbcTemplate db;
    @Autowired AuthTestData data;
    @Autowired Clock clock;
    long admin, lecturer, batch;

    @BeforeEach void setup() throws Exception {
        cleanup();
        admin = data.user("manual_admin", "ADMIN", UserStatus.ACTIVE).getId();
        lecturer = data.user("manual_lecturer", "LECTURER", UserStatus.ACTIVE).getId();
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

    @ParameterizedTest @ValueSource(strings = {"MANUAL", "REGULAR"})
    void createsAndEditsOneOffSessionInEitherMode(String mode) throws Exception {
        db.update("update course_batches set schedule_mode=? where id=?", mode, batch);
        var request = request().put("topic", "  Kitchen safety  ").put("classroom", "  Room 1  ").put("remarks", "   ");
        var saved = create(request, admin);
        long id = saved.path("id").asLong();
        assertThat(id).isPositive();
        assertThat(saved.path("status").asText()).isEqualTo("SCHEDULED");
        assertThat(saved.path("topic").asText()).isEqualTo("Kitchen safety");
        assertThat(saved.path("remarks").isNull()).isTrue();
        assertThat(saved.path("sourceScheduleId").isNull()).isTrue();
        var edit = request.put("version", saved.path("version").asLong()).put("sessionDate", "2026-07-31")
                .put("startTime", "03:00").put("endTime", "05:00").put("topic", "Practical class");
        var updated = body(call(put("/api/v1/sessions/" + id), edit, admin).andExpect(status().isOk()));
        assertThat(updated.path("createdAt")).isEqualTo(saved.path("createdAt"));
        assertThat(updated.path("version").asLong()).isEqualTo(saved.path("version").asLong() + 1);
        assertThat(db.queryForObject("select cast(start_time as varchar) from class_sessions where id=?", String.class, id)).startsWith("03:00");
        call(get("/api/v1/sessions/" + id), null, admin).andExpect(jsonPath("$.startTime").value("03:00:00"));
        assertThat(audits("SESSION_CREATED")).isEqualTo(1);
        assertThat(audits("SESSION_UPDATED")).isEqualTo(1);
        var oldValue = db.queryForObject("select cast(old_value_json as varchar) from audit_logs where action='SESSION_UPDATED'", String.class);
        assertThat(oldValue).contains("Kitchen safety");
    }

    @ParameterizedTest @ValueSource(strings = {"BEFORE", "AFTER", "EQUAL", "REVERSED", "NO_DATE", "NO_BATCH", "NO_START", "NEGATIVE_TEACHER", "TOPIC", "CLASSROOM", "REMARKS"})
    void rejectsInvalidCreateAndEditWithoutChangingData(String kind) throws Exception {
        var saved = create(request(), admin);
        var invalid = request().put("version", saved.path("version").asLong());
        switch (kind) {
            case "BEFORE" -> invalid.put("sessionDate", "2026-06-30");
            case "AFTER" -> invalid.put("sessionDate", "2026-08-01");
            case "EQUAL" -> invalid.put("endTime", "09:00");
            case "REVERSED" -> invalid.put("endTime", "08:00");
            case "NO_DATE" -> invalid.remove("sessionDate");
            case "NO_BATCH" -> invalid.remove("batchId");
            case "NO_START" -> invalid.remove("startTime");
            case "NEGATIVE_TEACHER" -> invalid.put("lecturerUserId", -1);
            case "TOPIC" -> invalid.put("topic", "x".repeat(301));
            case "CLASSROOM" -> invalid.put("classroom", "x".repeat(151));
            case "REMARKS" -> invalid.put("remarks", "x".repeat(2001));
        }
        call(post("/api/v1/sessions"), invalid, admin).andExpect(status().isBadRequest());
        call(put("/api/v1/sessions/" + saved.path("id").asLong()), invalid, admin).andExpect(status().isBadRequest());
        assertThat(count()).isEqualTo(1);
        assertThat(audits("SESSION_UPDATED")).isZero();
    }

    @Test void overlapsIncludeCompletedSessionsButAllowAdjacentAndRetiredSlots() throws Exception {
        var first = create(request(), admin);
        call(post("/api/v1/sessions"), request(), admin).andExpect(status().isConflict());
        assign("2020-01-01", "2099-12-31");
        call(post("/api/v1/sessions"), request().put("lecturerUserId", lecturer).put("startTime", "10:00"), admin).andExpect(status().isConflict());
        var adjacent = create(request().put("startTime", "13:00").put("endTime", "15:00"), admin);
        var edit = request().put("startTime", "12:00").put("endTime", "14:00").put("version", adjacent.path("version").asLong());
        call(put("/api/v1/sessions/" + adjacent.path("id").asLong()), edit, admin).andExpect(status().isConflict());
        db.update("update class_sessions set status='COMPLETED' where id=?", first.path("id").asLong());
        call(post("/api/v1/sessions"), request(), admin).andExpect(status().isConflict());
        db.update("update class_sessions set status='CANCELLED', cancellation_reason='Holiday' where id=?", first.path("id").asLong());
        var replacement = create(request(), admin);
        db.update("update class_sessions set status='RESCHEDULED', rescheduling_reason='Fixture move' where id=?", replacement.path("id").asLong());
        create(request(), admin);
        assertThat(count()).isEqualTo(4);
    }

    @Test void overlapChecksPreserveEarlyMorningLocalTimesAndScopeByDate() throws Exception {
        var early = request().put("startTime", "03:00").put("endTime", "05:00");
        var saved = create(early, admin);
        call(post("/api/v1/sessions"), request().put("startTime", "04:00").put("endTime", "06:00"), admin)
                .andExpect(status().isConflict());
        create(early.deepCopy().put("sessionDate", "2026-07-02"), admin);
        call(put("/api/v1/sessions/" + saved.path("id").asLong()), early.put("version", 0).put("topic", "Same slot"), admin)
                .andExpect(status().isOk());
    }

    @Test void staleMissingVersionAndBatchReassignmentAreRejected() throws Exception {
        var saved = create(request(), admin);
        String url = "/api/v1/sessions/" + saved.path("id").asLong();
        call(put(url), request(), admin).andExpect(status().isConflict());
        var edit = request().put("version", saved.path("version").asLong()).put("topic", "Updated");
        call(put(url), edit, admin).andExpect(status().isOk());
        call(put(url), edit.put("topic", "Stale"), admin).andExpect(status().isConflict());
        call(put(url), edit.put("batchId", batch + 1).put("version", 1), admin).andExpect(status().isBadRequest());
        call(get(url), null, admin).andExpect(jsonPath("$.topic").value("Updated"));
    }

    @ParameterizedTest @ValueSource(strings = {"COMPLETED", "CANCELLED", "RESCHEDULED", "ATTENDANCE"})
    void protectsHistoricalAndSubmittedSessions(String state) throws Exception {
        var saved = create(request(), admin);
        long id = saved.path("id").asLong();
        if (state.equals("ATTENDANCE")) db.update("update class_sessions set attendance_submitted_at=current_timestamp where id=?", id);
        else db.update("update class_sessions set status=?, cancellation_reason='Test history', rescheduling_reason='Fixture move' where id=?", state, id);
        call(put("/api/v1/sessions/" + id), request().put("version", 0).put("topic", "Changed"), admin).andExpect(status().isConflict());
        assertThat(audits("SESSION_UPDATED")).isZero();
    }

    @ParameterizedTest @ValueSource(strings = {"COMPLETED", "CANCELLED", "INACTIVE_BRANCH"})
    void rejectsWritesInUnavailableBatches(String state) throws Exception {
        var saved = create(request(), admin);
        if (state.equals("INACTIVE_BRANCH")) db.update("update branches set status='INACTIVE' where id=1");
        else db.update("update course_batches set status=? where id=?", state, batch);
        call(post("/api/v1/sessions"), request(), admin).andExpect(status().isBadRequest());
        call(put("/api/v1/sessions/" + saved.path("id").asLong()), request().put("version", 0), admin).andExpect(status().isBadRequest());
    }

    @Test void lecturerNeedsCurrentAssignmentAndBranchForCreateAndEdit() throws Exception {
        call(post("/api/v1/sessions"), request(), lecturer).andExpect(status().isForbidden());
        assign("2020-01-01", "2099-12-31");
        var saved = create(request(), lecturer);
        String url = "/api/v1/sessions/" + saved.path("id").asLong();
        call(put(url), request().put("version", 0).put("topic", "Lecturer edit"), lecturer).andExpect(status().isOk());
        String tomorrow = LocalDate.now(clock.withZone(ZoneId.of("Asia/Colombo"))).plusDays(1).toString();
        db.update("update batch_lecturers set assignment_start_date=cast(? as date)", tomorrow);
        call(post("/api/v1/sessions"), request(), lecturer).andExpect(status().isForbidden());
        call(put(url), request().put("version", 1), lecturer).andExpect(status().isForbidden());
        db.update("update batch_lecturers set assignment_start_date=date '2020-01-01', assignment_end_date=date '2020-01-02'");
        call(post("/api/v1/sessions"), request(), lecturer).andExpect(status().isForbidden());
        db.update("update batch_lecturers set assignment_end_date=date '2099-12-31', status='INACTIVE'");
        call(put(url), request().put("version", 1), lecturer).andExpect(status().isForbidden());
        db.update("update batch_lecturers set status='ACTIVE'");
        db.update("delete from user_branches where user_id=?", lecturer);
        call(post("/api/v1/sessions"), request(), lecturer).andExpect(status().isForbidden());
        call(put(url), request().put("version", 1), lecturer).andExpect(status().isForbidden());
    }

    @Test void namedLecturerMustBeEligibleOnProposedDate() throws Exception {
        var request = request().put("lecturerUserId", lecturer);
        call(post("/api/v1/sessions"), request, admin).andExpect(status().isBadRequest());
        assign("2026-07-01", "2026-07-15");
        var saved = create(request, admin);
        call(put("/api/v1/sessions/" + saved.path("id").asLong()), request.put("version", 0).put("sessionDate", "2026-07-31"), admin)
                .andExpect(status().isBadRequest());
        request.put("sessionDate", "2026-07-02");
        db.update("update users set status='DISABLED' where id=?", lecturer);
        call(post("/api/v1/sessions"), request, admin).andExpect(status().isBadRequest());
        db.update("update users set status='ACTIVE' where id=?", lecturer);
        db.update("delete from user_branches where user_id=?", lecturer);
        call(post("/api/v1/sessions"), request, admin).andExpect(status().isBadRequest());
        call(post("/api/v1/sessions"), request.put("lecturerUserId", admin), admin).andExpect(status().isBadRequest());
    }

    @Test void branchHeaderRoleAndMissingResourceChecksApplyToWrites() throws Exception {
        var saved = create(request(), admin);
        String url = "/api/v1/sessions/" + saved.path("id").asLong();
        long otherBranch = data.branch("MAN-OTHER").getId();
        long otherAdmin = data.user("other_admin", "ADMIN", UserStatus.ACTIVE, "MAN-OTHER").getId();
        long superAdmin = data.user("manual_super", "SUPER_ADMIN", UserStatus.ACTIVE, "MAN-OTHER").getId();
        call(post("/api/v1/sessions"), request(), otherAdmin).andExpect(status().isForbidden());
        call(put(url), request().put("version", 0), otherAdmin).andExpect(status().isForbidden());
        call(post("/api/v1/sessions").header("X-Active-Branch-Id", otherBranch), request(), admin).andExpect(status().isForbidden());
        call(put(url).header("X-Active-Branch-Id", otherBranch), request().put("version", 0), admin).andExpect(status().isForbidden());
        call(put(url), request().put("version", 0).put("topic", "Super edit"), superAdmin).andExpect(status().isOk());
        create(request().put("sessionDate", "2026-07-02"), superAdmin);
        call(post("/api/v1/sessions"), request().put("batchId", 9999999), admin).andExpect(status().isNotFound());
        call(put("/api/v1/sessions/9999999"), request().put("version", 0), admin).andExpect(status().isNotFound());
        mvc.perform(post("/api/v1/sessions").contentType(MediaType.APPLICATION_JSON).content(request().toString())).andExpect(status().isUnauthorized());
        db.update("delete from user_roles where user_id=?", otherAdmin);
        call(post("/api/v1/sessions"), request(), otherAdmin).andExpect(status().isForbidden());
    }

    @Test void generatedSessionEditsKeepOriginAndDoNotRegenerateOriginalDate() throws Exception {
        var generation = generationRequest();
        var result = body(call(post(generationUrl()), generation, admin).andExpect(status().isOk()));
        long id = result.path("createdSessionIds").get(0).asLong();
        var original = body(call(get("/api/v1/sessions/" + id), null, admin));
        call(put("/api/v1/sessions/" + id), request().put("version", 0).put("sessionDate", "2026-07-02"), admin)
                .andExpect(status().isOk()).andExpect(jsonPath("$.generationDate").value("2026-07-01"))
                .andExpect(jsonPath("$.sourceScheduleId").value(original.path("sourceScheduleId").asLong()));
        call(post(generationUrl()), generation, admin).andExpect(status().isOk()).andExpect(jsonPath("$.createdCount").value(0));
        assertThat(count()).isEqualTo(1);
    }

    @Test void concurrentOverlappingCreatesAreSerialized() throws Exception {
        var results = race(() -> responseCode(post("/api/v1/sessions"), request()),
                () -> responseCode(post("/api/v1/sessions"), request().put("startTime", "10:00").put("endTime", "14:00")));
        assertThat(results).containsExactlyInAnyOrder(201, 409);
        assertThat(count()).isEqualTo(1);
        assertThat(audits("SESSION_CREATED")).isEqualTo(1);
    }

    @Test void concurrentEditsCannotSilentlyOverwriteEachOther() throws Exception {
        var saved = create(request(), admin);
        String url = "/api/v1/sessions/" + saved.path("id").asLong();
        assertThat(race(() -> responseCode(put(url), request().put("version", 0).put("topic", "A")),
                () -> responseCode(put(url), request().put("version", 0).put("topic", "B")))).containsExactlyInAnyOrder(200, 409);
        assertThat(audits("SESSION_UPDATED")).isEqualTo(1);
    }

    @Test void manualCreateAndBulkGenerationCannotCreateOverlappingSessions() throws Exception {
        var generation = generationRequest();
        var results = race(() -> responseCode(post(generationUrl()), generation),
                () -> responseCode(post("/api/v1/sessions"), request().put("startTime", "10:00").put("endTime", "14:00")));
        assertThat(results).contains(409);
        assertThat(results).anyMatch(code -> code == 200 || code == 201);
        assertThat(count()).isEqualTo(1);
    }

    @Test void auditFailureRollsBackCreateAndEdit() throws Exception {
        var saved = create(request(), admin);
        db.execute("alter table audit_logs add constraint test_session_audit_failure check (action not in ('SESSION_CREATED','SESSION_UPDATED') or entity_id='" + saved.path("id").asLong() + "' and action='SESSION_CREATED')");
        try {
            assertThatThrownBy(() -> call(post("/api/v1/sessions"), request().put("sessionDate", "2026-07-02"), admin))
                    .hasRootCauseInstanceOf(java.sql.SQLException.class);
            assertThatThrownBy(() -> call(put("/api/v1/sessions/" + saved.path("id").asLong()), request().put("version", 0).put("topic", "Rollback"), admin))
                    .hasRootCauseInstanceOf(java.sql.SQLException.class);
            assertThat(count()).isEqualTo(1);
            call(get("/api/v1/sessions/" + saved.path("id").asLong()), null, admin).andExpect(jsonPath("$.version").value(0)).andExpect(jsonPath("$.topic").isEmpty());
        } finally { db.execute("alter table audit_logs drop constraint test_session_audit_failure"); }
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
