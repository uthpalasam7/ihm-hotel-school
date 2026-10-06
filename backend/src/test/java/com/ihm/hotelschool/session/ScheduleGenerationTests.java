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
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ScheduleGenerationTests {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired JdbcTemplate db;
    @Autowired AuthTestData data;
    @Autowired ClassSessionRepository sessions;
    @Autowired com.ihm.hotelschool.batch.CourseBatchRepository batches;
    @Autowired com.ihm.hotelschool.user.UserRepository users;
    @Autowired org.springframework.transaction.PlatformTransactionManager transactions;
    long admin, lecturer, batch;

    @BeforeEach void setup() throws Exception {
        cleanup();
        admin = data.user("schedule_admin", "ADMIN", UserStatus.ACTIVE).getId();
        lecturer = data.user("schedule_lecturer", "LECTURER", UserStatus.ACTIVE).getId();
        batch = batch("SCHEDULE-01", 1L);
    }

    @AfterEach void cleanup() {
        for (var table : List.of("class_sessions", "batch_schedules", "document_deliveries", "student_card_events", "student_cards",
                "student_charges", "enrollments", "batch_lecturers", "fee_plans", "course_batches", "courses", "refresh_tokens",
                "audit_logs", "students", "user_roles", "user_branches", "users")) db.update("delete from " + table);
        db.update("delete from branches where id <> 1");
        db.update("update branches set status='ACTIVE' where id=1");
    }

    @Test void previewMatchesSavedDatesAndExclusionsAndRepeatIsSafe() throws Exception {
        pattern(1, "09:00", "13:00", null);
        pattern(3, "09:00", "13:00", null);
        ObjectNode request = range("2026-07-01", "2026-07-31");
        request.putArray("excludeDates").add("2026-07-08");
        var preview = preview(request);
        assertThat(preview.path("createCount").asInt()).isEqualTo(8);
        assertThat(count("class_sessions")).isZero();
        List<String> proposed = new ArrayList<>();
        preview.path("sessions").forEach(e -> proposed.add(e.path("sessionDate").asText()));
        var result = generate(request, preview.path("previewToken").asText());
        assertThat(result.path("createdCount").asInt()).isEqualTo(8);
        assertThat(db.queryForList("select cast(session_date as varchar) from class_sessions order by session_date", String.class)).containsExactlyElementsOf(proposed);
        var again = generate(request, preview.path("previewToken").asText());
        assertThat(again.path("createdCount").asInt()).isZero();
        assertThat(again.path("skippedCount").asInt()).isEqualTo(8);
        assertThat(count("class_sessions")).isEqualTo(8);
        assertThat(db.queryForObject("select count(*) from audit_logs where action='SESSIONS_GENERATED'", Long.class)).isEqualTo(1);
        long first = result.path("createdSessionIds").get(0).asLong();
        call(get("/api/v1/sessions/" + first), null, admin).andExpect(jsonPath("$.generationDate").value(proposed.get(0)))
                .andExpect(jsonPath("$.sourceScheduleId").isNumber());
    }

    @Test void singleDayPreviewIncludesBatchStartAndEnd() throws Exception {
        pattern(3, "09:00", "13:00", null);
        pattern(5, "09:00", "13:00", null);
        assertThat(preview(range("2026-07-01", "2026-07-01")).path("createCount").asInt()).isEqualTo(1);
        assertThat(preview(range("2026-07-31", "2026-07-31")).path("createCount").asInt()).isEqualTo(1);
        ObjectNode excluded = range("2026-07-01", "2026-07-01");
        excluded.putArray("excludeDates").add("2026-07-01");
        var p = preview(excluded);
        assertThat(p.path("createCount").asInt()).isZero();
        assertThat(generate(excluded, p.path("previewToken").asText()).path("createdCount").asInt()).isZero();
    }

    @Test void patternUpdateRequiresVersionAndDoesNotRewriteGeneratedHistory() throws Exception {
        var pattern = pattern(3, "09:00", "13:00", null);
        var request = range("2026-07-01", "2026-07-31");
        var oldPreview = preview(request);
        generate(request, oldPreview.path("previewToken").asText());
        var edit = patternRequest(3, "14:00", "17:00", null);
        call(put("/api/v1" + path("/schedules/" + pattern.path("id").asLong())), edit, admin).andExpect(status().isConflict());
        edit.put("version", pattern.path("version").asLong());
        var changed = body(call(put("/api/v1" + path("/schedules/" + pattern.path("id").asLong())), edit, admin).andExpect(status().isOk()));
        assertThat(changed.path("version").asLong()).isEqualTo(1);
        request.put("previewToken", oldPreview.path("previewToken").asText());
        call(post("/api/v1" + path("/sessions/generate")), request, admin).andExpect(status().isConflict());
        assertThat(db.queryForList("select cast(start_time as varchar) from class_sessions", String.class)).allMatch(s -> s.startsWith("09:00"));
        var fresh = preview(request);
        assertThat(fresh.path("skipCount").asInt()).isEqualTo(5);
        generate(request, fresh.path("previewToken").asText());
        assertThat(count("class_sessions")).isEqualTo(5);
        call(delete("/api/v1" + path("/schedules/" + pattern.path("id").asLong()) + "?version=1"), null, admin).andExpect(status().isNoContent());
        assertThat(count("batch_schedules")).isEqualTo(1);
        assertThat(count("class_sessions")).isEqualTo(5);
    }

    @Test void cancelledAndRescheduledSlotsAreNotRecreated() throws Exception {
        pattern(3, "09:00", "13:00", null);
        var request = range("2026-07-01", "2026-07-31");
        var p = preview(request);
        generate(request, p.path("previewToken").asText());
        db.update("update class_sessions set status='CANCELLED',cancellation_reason='Holiday' where session_date='2026-07-01'");
        db.update("update class_sessions set status='RESCHEDULED', rescheduling_reason='Fixture move' where session_date='2026-07-08'");
        assertThat(generate(request, p.path("previewToken").asText()).path("createdCount").asInt()).isZero();
        assertThat(count("class_sessions")).isEqualTo(5);
    }

    @Test void matchingManualSessionIsSkippedButOverlapBlocksWholeGeneration() throws Exception {
        pattern(3, "09:00", "13:00", null);
        insertSession("2026-07-01", "09:00", "13:00", "SCHEDULED", null);
        var request = range("2026-07-01", "2026-07-31");
        var p = preview(request);
        assertThat(p.path("skipCount").asInt()).isEqualTo(1);
        assertThat(p.path("createCount").asInt()).isEqualTo(4);
        insertSession("2026-07-08", "10:00", "14:00", "SCHEDULED", null);
        request.put("previewToken", p.path("previewToken").asText());
        call(post("/api/v1" + path("/sessions/generate")), request, admin).andExpect(status().isConflict());
        assertThat(count("class_sessions")).isEqualTo(2);
        assertThat(preview(request).path("conflictCount").asInt()).isEqualTo(1);
    }

    @Test void previewTokenIsRequiredBoundToActorRequestAndPattern() throws Exception {
        pattern(3, "09:00", "13:00", null);
        var request = range("2026-07-01", "2026-07-31");
        call(post("/api/v1" + path("/sessions/generate")), request, admin).andExpect(status().isConflict());
        var p = preview(request);
        request.put("previewToken", p.path("previewToken").asText());
        long otherAdmin = data.user("other_schedule_admin", "ADMIN", UserStatus.ACTIVE).getId();
        call(post("/api/v1" + path("/sessions/generate")), request, otherAdmin).andExpect(status().isConflict());
        request.put("toDate", "2026-07-30");
        call(post("/api/v1" + path("/sessions/generate")), request, admin).andExpect(status().isConflict());
        assertThat(count("class_sessions")).isZero();
    }

    @Test void lecturerEligibilityIsCheckedForEveryProposedDateAndAgainOnGeneration() throws Exception {
        var assignment = assign("2026-07-01", "2026-07-15");
        pattern(3, "09:00", "13:00", lecturer);
        var all = range("2026-07-01", "2026-07-31");
        var p = preview(all);
        assertThat(p.path("conflictCount").asInt()).isEqualTo(2);
        all.put("previewToken", p.path("previewToken").asText());
        call(post("/api/v1" + path("/sessions/generate")), all, admin).andExpect(status().isConflict());
        var valid = range("2026-07-01", "2026-07-15");
        var validPreview = preview(valid);
        assertThat(validPreview.path("createCount").asInt()).isEqualTo(3);
        db.update("update batch_lecturers set status='INACTIVE' where id=?", assignment);
        valid.put("previewToken", validPreview.path("previewToken").asText());
        call(post("/api/v1" + path("/sessions/generate")), valid, admin).andExpect(status().isConflict());
        assertThat(count("class_sessions")).isZero();
    }

    @Test void accessRulesApplyToPatternsPreviewAndGeneration() throws Exception {
        pattern(3, "09:00", "13:00", null);
        call(get("/api/v1" + path("/schedules")), null, lecturer).andExpect(status().isForbidden());
        assign("2020-01-01", "2099-12-31");
        call(get("/api/v1" + path("/schedules")), null, lecturer).andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(1));
        call(post("/api/v1" + path("/schedules")), patternRequest(5, "09:00", "13:00", null), lecturer).andExpect(status().isForbidden());
        call(post("/api/v1" + path("/sessions/preview")), range("2026-07-01", "2026-07-31"), lecturer).andExpect(status().isForbidden());
        call(post("/api/v1" + path("/sessions/generate")), range("2026-07-01", "2026-07-31"), lecturer).andExpect(status().isForbidden());
        long otherBranch = data.branch("SCHEDULE-OTHER").getId();
        long otherAdmin = data.user("unrelated_admin", "ADMIN", UserStatus.ACTIVE, "SCHEDULE-OTHER").getId();
        call(get("/api/v1" + path("/schedules")), null, otherAdmin).andExpect(status().isForbidden());
        call(post("/api/v1" + path("/sessions/preview")), range("2026-07-01", "2026-07-31"), otherAdmin).andExpect(status().isForbidden());
        call(post("/api/v1" + path("/sessions/preview")).header("X-Active-Branch-Id", otherBranch), range("2026-07-01", "2026-07-31"), admin)
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/v1" + path("/schedules"))).andExpect(status().isUnauthorized());
    }

    @Test void duplicateAndOverlappingPatternsFailButAdjacentPatternsWork() throws Exception {
        pattern(3, "09:00", "13:00", null);
        call(post("/api/v1" + path("/schedules")), patternRequest(3, "09:00", "13:00", null), admin).andExpect(status().isConflict());
        call(post("/api/v1" + path("/schedules")), patternRequest(3, "12:00", "14:00", null), admin).andExpect(status().isConflict());
        pattern(3, "13:00", "15:00", null);
        assertThat(count("batch_schedules")).isEqualTo(2);
    }

    @Test void invalidPatternsAndUnassignedLecturerAreRejected() throws Exception {
        for (var request : List.of(patternRequest(0, "09:00", "13:00", null), patternRequest(8, "09:00", "13:00", null),
                patternRequest(3, "13:00", "09:00", null), patternRequest(3, "09:00", "13:00", lecturer))) {
            call(post("/api/v1" + path("/schedules")), request, admin).andExpect(status().isBadRequest());
        }
        assertThat(count("batch_schedules")).isZero();
    }

    @ParameterizedTest
    @ValueSource(strings = {"REVERSED", "BEFORE_BATCH", "AFTER_BATCH", "TOO_LONG", "EXCLUDED_OUTSIDE", "NULL_EXCLUSION", "NO_PATTERNS", "MANUAL", "CANCELLED"})
    void invalidGenerationInputIsRejected(String kind) throws Exception {
        if (!kind.equals("NO_PATTERNS")) pattern(3, "09:00", "13:00", null);
        var request = range("2026-07-01", "2026-07-31");
        switch (kind) {
            case "REVERSED" -> request.put("toDate", "2026-06-30");
            case "BEFORE_BATCH" -> request.put("fromDate", "2026-06-30");
            case "AFTER_BATCH" -> request.put("toDate", "2026-08-01");
            case "TOO_LONG" -> request.put("toDate", "2027-07-02");
            case "EXCLUDED_OUTSIDE" -> request.putArray("excludeDates").add("2026-08-01");
            case "NULL_EXCLUSION" -> request.putArray("excludeDates").addNull();
            case "MANUAL" -> db.update("update course_batches set schedule_mode='MANUAL' where id=?", batch);
            case "CANCELLED" -> db.update("update course_batches set status='CANCELLED' where id=?", batch);
        }
        call(post("/api/v1" + path("/sessions/preview")), request, admin).andExpect(status().isBadRequest());
        assertThat(count("class_sessions")).isZero();
    }

    @Test void concurrencyCreatesEachSessionOnlyOnce() throws Exception {
        pattern(3, "09:00", "13:00", null);
        var request = range("2026-07-01", "2026-07-31");
        request.put("previewToken", preview(request).path("previewToken").asText());
        var gate = new CountDownLatch(1);
        var pool = Executors.newFixedThreadPool(2);
        try {
            Callable<JsonNode> task = () -> { gate.await(); return body(call(post("/api/v1" + path("/sessions/generate")), request, admin).andExpect(status().isOk())); };
            var a = pool.submit(task); var b = pool.submit(task); gate.countDown();
            assertThat(a.get(20, TimeUnit.SECONDS).path("createdCount").asInt() + b.get(20, TimeUnit.SECONDS).path("createdCount").asInt()).isEqualTo(5);
            assertThat(count("class_sessions")).isEqualTo(5);
        } finally { pool.shutdownNow(); }
    }

    @Test void concurrentPatternCreationRejectsDuplicate() throws Exception {
        var gate = new CountDownLatch(1);
        var pool = Executors.newFixedThreadPool(2);
        try {
            Callable<Integer> task = () -> { gate.await(); return call(post("/api/v1" + path("/schedules")), patternRequest(3, "09:00", "13:00", null), admin).andReturn().getResponse().getStatus(); };
            var a = pool.submit(task); var b = pool.submit(task); gate.countDown();
            assertThat(List.of(a.get(20, TimeUnit.SECONDS), b.get(20, TimeUnit.SECONDS))).containsExactlyInAnyOrder(201, 409);
            assertThat(count("batch_schedules")).isEqualTo(1);
        } finally { pool.shutdownNow(); }
    }

    @Test void databaseFailureRollsBackAllGeneratedSessionsAndAudit() throws Exception {
        pattern(3, "09:00", "13:00", null);
        var request = range("2026-07-01", "2026-07-31");
        request.put("previewToken", preview(request).path("previewToken").asText());
        db.execute("alter table class_sessions add constraint test_generation_failure check (session_date <> date '2026-07-08')");
        try {
            assertThatThrownBy(() -> call(post("/api/v1" + path("/sessions/generate")), request, admin)).hasRootCauseInstanceOf(java.sql.SQLException.class);
            assertThat(count("class_sessions")).isZero();
            assertThat(db.queryForObject("select count(*) from audit_logs where action='SESSIONS_GENERATED'", Long.class)).isZero();
        } finally { db.execute("alter table class_sessions drop constraint test_generation_failure"); }
    }

    @Test void databaseProtectsNullLecturerActiveSlotsAndGenerationOrigins() throws Exception {
        insertSession("2026-07-01", "09:00", "13:00", "SCHEDULED", null);
        assertThatThrownBy(() -> insertSession("2026-07-01", "09:00", "13:00", "SCHEDULED", null)).isInstanceOf(DataIntegrityViolationException.class);
        db.update("update class_sessions set status='CANCELLED', cancellation_reason='Holiday'");
        insertSession("2026-07-01", "09:00", "13:00", "SCHEDULED", null);
        var pattern = pattern(3, "14:00", "17:00", null);
        var request = range("2026-07-01", "2026-07-01");
        generate(request, preview(request).path("previewToken").asText());
        assertThatThrownBy(() -> db.update("update class_sessions set source_schedule_id=?,generation_date=date '2026-07-01' where source_schedule_id is null", pattern.path("id").asLong()))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test void existingSessionsPreventBatchIdentityAndDateChanges() throws Exception {
        pattern(3, "09:00", "13:00", null);
        var request = range("2026-07-01", "2026-07-31");
        generate(request, preview(request).path("previewToken").asText());
        var batchResponse = body(call(get("/api/v1/batches/" + batch), null, admin));
        var edit = (ObjectNode) batchResponse.deepCopy();
        edit.put("courseId", batchResponse.path("course").path("id").asLong());
        edit.put("branchId", batchResponse.path("branch").path("id").asLong());
        edit.put("startDate", "2026-07-02");
        call(put("/api/v1/batches/" + batch), edit, admin).andExpect(status().isConflict());
        edit.put("startDate", "2026-07-01").put("batchNumber", "CHANGED");
        call(put("/api/v1/batches/" + batch), edit, admin).andExpect(status().isConflict());
        edit.put("batchNumber", "SCHEDULE-01").put("scheduleMode", "MANUAL");
        call(put("/api/v1/batches/" + batch), edit, admin).andExpect(status().isConflict());
    }

    @Test void previewCandidateCountIsBounded() throws Exception {
        db.update("update course_batches set end_date=date '2027-07-01' where id=?", batch);
        for (int day = 1; day <= 7; day++) {
            pattern(day, "09:00", "10:00", null);
            pattern(day, "10:00", "11:00", null);
            pattern(day, "11:00", "12:00", null);
        }
        call(post("/api/v1" + path("/sessions/preview")), range("2026-07-01", "2027-07-01"), admin)
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.message").value("Preview exceeds 1000 sessions; use a smaller date range"));
        assertThat(count("class_sessions")).isZero();
    }

    @Test void localClassTimesAreStoredWithoutUtcConversion() throws Exception {
        var p = pattern(3, "03:00", "09:00", null);
        assertThat(db.queryForObject("select cast(start_time as varchar) from batch_schedules where id=?", String.class, p.path("id").asLong())).startsWith("03:00");
        var request = range("2026-07-01", "2026-07-01");
        var result = generate(request, preview(request).path("previewToken").asText());
        long id = result.path("createdSessionIds").get(0).asLong();
        assertThat(db.queryForObject("select cast(start_time as varchar) from class_sessions where id=?", String.class, id)).startsWith("03:00");
        call(get("/api/v1/sessions/" + id), null, admin).andExpect(jsonPath("$.startTime").value("03:00:00"))
                .andExpect(jsonPath("$.endTime").value("09:00:00"));
    }

    private long batch(String number, long branch) throws Exception {
        long course = body(call(post("/api/v1/courses"), Map.of("name", "Regular course", "shortCode", number, "status", "ACTIVE"), admin).andExpect(status().isCreated())).path("id").asLong();
        return body(call(post("/api/v1/batches"), Map.of("courseId", course, "branchId", branch, "batchNumber", number,
                "startDate", "2026-07-01", "endDate", "2026-07-31", "durationMonths", 1, "scheduleMode", "REGULAR", "status", "UPCOMING"), admin)
                .andExpect(status().isCreated())).path("id").asLong();
    }
    private ObjectNode patternRequest(int day, String start, String end, Long teacher) {
        var request = json.createObjectNode().put("dayOfWeek", day).put("startTime", start).put("endTime", end).put("status", "ACTIVE");
        if (teacher != null) request.put("defaultLecturerUserId", teacher);
        return request;
    }
    private JsonNode pattern(int day, String start, String end, Long teacher) throws Exception {
        return body(call(post("/api/v1" + path("/schedules")), patternRequest(day, start, end, teacher), admin).andExpect(status().isCreated()));
    }
    private long assign(String from, String to) throws Exception {
        return body(call(post("/api/v1" + path("/lecturers")), Map.of("lecturerUserId", lecturer, "assignmentStartDate", from, "assignmentEndDate", to, "status", "ACTIVE"), admin)
                .andExpect(status().isCreated())).path("id").asLong();
    }
    private ObjectNode range(String from, String to) { return json.createObjectNode().put("fromDate", from).put("toDate", to); }
    private JsonNode preview(ObjectNode request) throws Exception { return body(call(post("/api/v1" + path("/sessions/preview")), request, admin).andExpect(status().isOk())); }
    private JsonNode generate(ObjectNode request, String token) throws Exception {
        request.put("previewToken", token);
        return body(call(post("/api/v1" + path("/sessions/generate")), request, admin).andExpect(status().isOk()));
    }
    private void insertSession(String date, String start, String end, String status, Long teacher) {
        new org.springframework.transaction.support.TransactionTemplate(transactions).execute(tx -> {
            var row = sessions.saveAndFlush(new ClassSession(batches.findById(batch).orElseThrow(),
                    java.time.LocalDate.parse(date), java.time.LocalTime.parse(start), java.time.LocalTime.parse(end),
                    teacher == null ? null : users.findById(teacher).orElseThrow(), null, null, null, java.time.Instant.now(), admin));
            if (!status.equals("SCHEDULED")) throw new IllegalArgumentException("Fixture expects a scheduled session");
            return row.getId();
        });
    }
    private String path(String suffix) { return "/batches/" + batch + suffix; }
    private ResultActions call(MockHttpServletRequestBuilder request, Object body, long actor) throws Exception {
        request.with(jwt().jwt(j -> j.claim("userId", actor)));
        if (body != null) request.contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(body));
        return mvc.perform(request);
    }
    private JsonNode body(ResultActions result) throws Exception { return json.readTree(result.andReturn().getResponse().getContentAsString()); }
    private long count(String table) { return db.queryForObject("select count(*) from " + table, Long.class); }
}
