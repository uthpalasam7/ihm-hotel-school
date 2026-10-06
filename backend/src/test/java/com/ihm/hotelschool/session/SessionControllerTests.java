package com.ihm.hotelschool.session;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.ihm.hotelschool.auth.AuthTestData;
import com.ihm.hotelschool.batch.*;
import com.ihm.hotelschool.branch.Branch;
import com.ihm.hotelschool.course.*;
import com.ihm.hotelschool.user.UserAccount;
import com.ihm.hotelschool.user.UserStatus;
import java.time.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class SessionControllerTests {
    @Autowired MockMvc mvc;
    @Autowired AuthTestData data;
    @Autowired CourseRepository courses;
    @Autowired CourseBatchRepository batches;
    @Autowired BatchLecturerRepository assignments;
    @Autowired ClassSessionRepository sessions;
    @Autowired BatchScheduleRepository schedules;
    @Autowired JdbcTemplate db;
    @Autowired Clock clock;
    UserAccount admin, lecturer, superAdmin;
    CourseBatch mainBatch, otherBatch;
    ClassSession first, second, foreign;
    LocalDate today;

    @BeforeEach void setup() {
        today = LocalDate.now(clock.withZone(ZoneId.of("Asia/Colombo")));
        admin = data.user("session_admin", "ADMIN", UserStatus.ACTIVE);
        lecturer = data.user("session_lecturer", "LECTURER", UserStatus.ACTIVE);
        superAdmin = data.user("session_super", "SUPER_ADMIN", UserStatus.ACTIVE);
        mainBatch = batch(data.branch("IHM-MAIN"), "SESSION-MAIN");
        otherBatch = batch(data.branch("SESSION-OTHER"), "SESSION-OTHER");
        first = session(mainBatch, today, lecturer);
        second = session(mainBatch, today.plusDays(1), null);
        foreign = session(otherBatch, today, lecturer);
    }

    @Test void adminGetsReadableScopedPagesAndDetail() throws Exception {
        as("?size=1", admin).andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.totalPages").value(2))
                .andExpect(jsonPath("$.content[0].id").value(first.getId()))
                .andExpect(jsonPath("$.content[0].batchNumber").value("SESSION-MAIN"))
                .andExpect(jsonPath("$.content[0].courseName").value("Session course"))
                .andExpect(jsonPath("$.content[0].branchName").exists())
                .andExpect(jsonPath("$.content[0].lecturerName").value(lecturer.getFullName()))
                .andExpect(jsonPath("$.content[0].status").value("SCHEDULED"))
                .andExpect(jsonPath("$.content[0].courseFee").doesNotExist());
        as("?size=1&page=1", admin).andExpect(jsonPath("$.content[0].id").value(second.getId()))
                .andExpect(jsonPath("$.content[0].lecturerUserId").doesNotExist());
        as("/" + first.getId(), admin).andExpect(status().isOk())
                .andExpect(jsonPath("$.topic").value("Kitchen safety"))
                .andExpect(jsonPath("$.classroom").value("Kitchen 1"))
                .andExpect(jsonPath("$.sessionDate").value(today.toString()))
                .andExpect(jsonPath("$.version").value(0));
        as("?size=999", admin).andExpect(jsonPath("$.size").value(100));
    }

    @Test void superAdminCanReadAcrossBranchesButHeaderFiltersDefaultList() throws Exception {
        as("", superAdmin).andExpect(jsonPath("$.totalElements").value(3));
        as("/" + foreign.getId(), superAdmin).andExpect(status().isOk());
        mvc.perform(get("/api/v1/sessions").header("X-Active-Branch-Id", mainBatch.getBranch().getId())
                .with(jwt().jwt(j -> j.claim("userId", superAdmin.getId()))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(2));
    }

    @Test void filtersCombineAndDateBoundsAreInclusive() throws Exception {
        as("?batchId=" + mainBatch.getId() + "&lecturerId=" + lecturer.getId()
                + "&dateFrom=" + today + "&dateTo=" + today + "&status=scheduled", admin)
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(1));
        as("?status=CANCELLED", admin).andExpect(jsonPath("$.totalElements").value(0));
        as("?dateFrom=" + today.plusDays(2), admin).andExpect(jsonPath("$.totalElements").value(0));
        as("?branchId=" + otherBatch.getBranch().getId(), superAdmin)
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test void branchAccessCannotBeBypassedUsingFiltersOrDetail() throws Exception {
        as("/" + foreign.getId(), admin).andExpect(status().isForbidden());
        as("?batchId=" + otherBatch.getId(), admin).andExpect(status().isForbidden());
        as("?branchId=" + otherBatch.getBranch().getId(), admin).andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/sessions/" + first.getId())
                .header("X-Active-Branch-Id", otherBatch.getBranch().getId())
                .with(jwt().jwt(j -> j.claim("userId", admin.getId())))).andExpect(status().isForbidden());
    }

    @Test void lecturerNeedsCurrentBatchAssignmentEvenIfNamedOnSession() throws Exception {
        as("", lecturer).andExpect(jsonPath("$.totalElements").value(0));
        as("/" + first.getId(), lecturer).andExpect(status().isForbidden());
        as("?batchId=" + mainBatch.getId(), lecturer).andExpect(status().isForbidden());
        assign(mainBatch, today, today, BatchLecturerStatus.ACTIVE);
        as("?size=1", lecturer).andExpect(jsonPath("$.totalElements").value(2));
        as("/" + second.getId(), lecturer).andExpect(status().isOk());
        // Batch assignment never overrides branch restrictions.
        assign(otherBatch, today.minusDays(1), null, BatchLecturerStatus.ACTIVE);
        as("", lecturer).andExpect(jsonPath("$.totalElements").value(2));
        as("/" + foreign.getId(), lecturer).andExpect(status().isForbidden());
    }

    @Test void lecturerReadsOnlyActiveColleagueAssignmentsInCurrentlyAssignedBatch() throws Exception {
        var colleague = data.user("session_colleague", "LECTURER", UserStatus.ACTIVE);
        var retired = data.user("session_retired", "LECTURER", UserStatus.ACTIVE);
        assign(mainBatch, today.minusDays(1), null, BatchLecturerStatus.ACTIVE);
        assignments.saveAndFlush(new BatchLecturer(mainBatch, colleague, today.plusDays(1), null,
                BatchLecturerStatus.ACTIVE, clock.instant(), admin.getId()));
        assignments.saveAndFlush(new BatchLecturer(mainBatch, retired, today.minusDays(5), today.minusDays(1),
                BatchLecturerStatus.INACTIVE, clock.instant(), admin.getId()));
        String path = "/api/v1/batches/" + mainBatch.getId() + "/lecturers";
        mvc.perform(get(path).with(jwt().jwt(j -> j.claim("userId", lecturer.getId()))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].lecturerUserId").value(lecturer.getId()))
                .andExpect(jsonPath("$[1].lecturerUserId").value(colleague.getId()));
        mvc.perform(get(path).with(jwt().jwt(j -> j.claim("userId", admin.getId()))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(3));
        mvc.perform(get("/api/v1/batches/" + otherBatch.getId() + "/lecturers")
                .with(jwt().jwt(j -> j.claim("userId", lecturer.getId()))))
                .andExpect(status().isForbidden());
        mvc.perform(post(path).contentType(MediaType.APPLICATION_JSON)
                .content("{\"lecturerUserId\":" + colleague.getId() + ",\"assignmentStartDate\":\"" + today
                        + "\",\"status\":\"ACTIVE\"}")
                .with(jwt().jwt(j -> j.claim("userId", lecturer.getId()))))
                .andExpect(status().isForbidden());
    }

    @Test void expiredOrRevokedLecturerCannotReadBatchAssignments() throws Exception {
        var assignment = assign(mainBatch, today.minusDays(2), today.minusDays(1), BatchLecturerStatus.ACTIVE);
        String path = "/api/v1/batches/" + mainBatch.getId() + "/lecturers";
        mvc.perform(get(path).with(jwt().jwt(j -> j.claim("userId", lecturer.getId()))))
                .andExpect(status().isForbidden());
        assignment.updateDetails(lecturer, today.minusDays(2), null, BatchLecturerStatus.ACTIVE, clock.instant(), admin.getId());
        assignments.flush();
        mvc.perform(get(path).with(jwt().jwt(j -> j.claim("userId", lecturer.getId()))))
                .andExpect(status().isOk());
        assignment.changeStatus(BatchLecturerStatus.INACTIVE, clock.instant(), admin.getId());
        assignments.flush();
        mvc.perform(get(path).with(jwt().jwt(j -> j.claim("userId", lecturer.getId()))))
                .andExpect(status().isForbidden());
    }

    @ParameterizedTest
    @ValueSource(strings = {"EXPIRED", "FUTURE", "INACTIVE"})
    void nonCurrentAssignmentsDoNotGrantAccess(String kind) throws Exception {
        assign(mainBatch, kind.equals("FUTURE") ? today.plusDays(1) : today.minusDays(5),
                kind.equals("EXPIRED") ? today.minusDays(1) : null,
                kind.equals("INACTIVE") ? BatchLecturerStatus.INACTIVE : BatchLecturerStatus.ACTIVE);
        as("", lecturer).andExpect(jsonPath("$.totalElements").value(0));
        as("/" + first.getId(), lecturer).andExpect(status().isForbidden());
    }

    @Test void revokingAssignmentRemovesAccessImmediately() throws Exception {
        var assignment = assign(mainBatch, today.minusDays(1), null, BatchLecturerStatus.ACTIVE);
        as("/" + first.getId(), lecturer).andExpect(status().isOk());
        assignment.changeStatus(BatchLecturerStatus.INACTIVE, clock.instant(), admin.getId());
        assignments.flush();
        as("/" + first.getId(), lecturer).andExpect(status().isForbidden());
        as("", lecturer).andExpect(jsonPath("$.totalElements").value(0));
    }

    @ParameterizedTest
    @ValueSource(strings = {"?status=INVALID", "?page=-1", "?size=0", "?batchId=0", "?lecturerId=-1",
            "?branchId=-2", "?dateFrom=2026-02-02&dateTo=2026-02-01", "?dateFrom=invalid", "?batchId=invalid", "?size=invalid"})
    void invalidFiltersAreRejected(String query) throws Exception {
        as(query, admin).andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("BAD_REQUEST"));
    }

    @Test void unauthenticatedAndRolelessRequestsFailAndMissingIdsReturn404() throws Exception {
        mvc.perform(get("/api/v1/sessions")).andExpect(status().isUnauthorized());
        db.update("delete from user_roles where user_id=?", lecturer.getId());
        // The actor is reloaded; clear the persistence context after the direct SQL change.
        entityManager.clear();
        as("", lecturer).andExpect(status().isForbidden());
        as("/999999999", admin).andExpect(status().isNotFound());
        as("?batchId=999999999", admin).andExpect(status().isNotFound());
    }
    @Autowired jakarta.persistence.EntityManager entityManager;

    @Test void scheduleMappingPersistsIsoWeekdayAndAuditVersion() {
        var schedule = schedules.saveAndFlush(new BatchSchedule(mainBatch, 1, LocalTime.of(9, 0),
                LocalTime.of(13, 0), lecturer, "Kitchen 1", clock.instant(), admin.getId()));
        assertThat(schedule.getId()).isNotNull();
        assertThat(db.queryForObject("select day_of_week from batch_schedules where id=?", Integer.class, schedule.getId())).isEqualTo(1);
        assertThat(schedule.getVersion()).isZero();
    }

    @ParameterizedTest
    @ValueSource(strings = {"start_time=end_time", "status='UNKNOWN'", "status='CANCELLED'",
            "batch_id=999999999", "lecturer_user_id=999999999", "original_session_id=id"})
    void sessionDatabaseConstraintsRejectInvalidRows(String update) {
        assertThatThrownBy(() -> db.update("update class_sessions set " + update + " where id=?", first.getId()))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test void originalSessionMustBelongToSameBatch() {
        assertThatThrownBy(() -> db.update("update class_sessions set original_session_id=? where id=?", foreign.getId(), first.getId()))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {"day_of_week=0", "day_of_week=8", "start_time=end_time", "status='UNKNOWN'"})
    void scheduleDatabaseConstraintsRejectInvalidRows(String update) {
        var schedule = schedules.saveAndFlush(new BatchSchedule(mainBatch, 7, LocalTime.of(9, 0),
                LocalTime.of(13, 0), null, null, clock.instant(), admin.getId()));
        assertThatThrownBy(() -> db.update("update batch_schedules set " + update + " where id=?", schedule.getId()))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    private CourseBatch batch(Branch branch, String number) {
        var course = courses.saveAndFlush(new Course("Session course", number, null, CourseStatus.ACTIVE, clock.instant(), admin.getId()));
        return batches.saveAndFlush(new CourseBatch(course, branch, number, today.minusMonths(1), today.plusMonths(2),
                3, ScheduleMode.REGULAR, BatchStatus.ACTIVE, null, clock.instant(), admin.getId()));
    }
    private ClassSession session(CourseBatch batch, LocalDate date, UserAccount teacher) {
        return sessions.saveAndFlush(new ClassSession(batch, date, LocalTime.of(9, 0), LocalTime.of(13, 0),
                teacher, "Kitchen safety", "Kitchen 1", null, clock.instant(), admin.getId()));
    }
    private BatchLecturer assign(CourseBatch batch, LocalDate from, LocalDate to, BatchLecturerStatus status) {
        return assignments.saveAndFlush(new BatchLecturer(batch, lecturer, from, to, status, clock.instant(), admin.getId()));
    }
    private ResultActions as(String suffix, UserAccount actor) throws Exception {
        return mvc.perform(get("/api/v1/sessions" + suffix).with(jwt().jwt(j -> j.claim("userId", actor.getId()))));
    }
}
