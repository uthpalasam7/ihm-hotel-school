package com.ihm.hotelschool.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.ihm.hotelschool.audit.AuditLogRepository;
import com.ihm.hotelschool.auth.AuthTestData;
import com.ihm.hotelschool.user.UserAccount;
import com.ihm.hotelschool.user.UserStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Sql(statements = {
		"delete from batch_lecturers",
		"delete from fee_plans",
		"delete from course_batches",
		"delete from courses",
		"delete from refresh_tokens",
		"delete from audit_logs",
		"delete from students",
		"delete from user_roles",
		"delete from user_branches",
		"delete from users",
		"delete from branches where id <> 1",
		"update branches set status = 'ACTIVE' where id = 1"
}, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class CourseBatchControllerTests {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private AuthTestData authTestData;

	@Autowired
	private AuditLogRepository auditLogRepository;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Test
	void adminCanCreateCourseAndDuplicateShortCodeIsRejected() throws Exception {
		authTestData.user("phase4_admin_course", "ADMIN", UserStatus.ACTIVE);
		String token = login("phase4_admin_course");

		MvcResult result = mockMvc.perform(post("/api/v1/courses")
						.header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"name":"Pastry & Bakery","shortCode":"pb","description":"Certificate course","status":"ACTIVE"}
								"""))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.shortCode").value("PB"))
				.andReturn();

		String courseId = extractJsonNumber(result.getResponse().getContentAsString(), "id");
		assertThat(auditLogRepository.countByActionAndEntityTypeAndEntityId("COURSE_CREATED", "Course", courseId)).isEqualTo(1);

		mockMvc.perform(post("/api/v1/courses")
						.header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"name":"Professional Bakery","shortCode":"PB","status":"ACTIVE"}
								"""))
				.andExpect(status().isConflict());
	}

	@Test
	void batchCreationValidatesDatesDuplicateNumberAndBranchAccess() throws Exception {
		Long cityBranchId = authTestData.branch("IHM-CITY").getId();
		authTestData.user("phase4_admin_batch", "ADMIN", UserStatus.ACTIVE, "IHM-MAIN");
		String token = login("phase4_admin_batch");
		String courseId = createCourse(token);

		mockMvc.perform(post("/api/v1/batches")
						.header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"courseId":%s,"branchId":1,"batchNumber":"2026/PB02","startDate":"2026-07-01","endDate":"2026-06-30","durationMonths":6,"scheduleMode":"REGULAR","status":"UPCOMING"}
								""".formatted(courseId)))
				.andExpect(status().isBadRequest());

		mockMvc.perform(post("/api/v1/batches")
						.header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"courseId":%s,"branchId":%s,"batchNumber":"2026/PB02","startDate":"2026-07-01","endDate":"2026-12-31","durationMonths":6,"scheduleMode":"REGULAR","status":"UPCOMING"}
								""".formatted(courseId, cityBranchId)))
				.andExpect(status().isForbidden());

		mockMvc.perform(post("/api/v1/batches")
						.header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"courseId":%s,"branchId":1,"batchNumber":"2026/PB02","startDate":"2026-07-01","endDate":"2026-12-31","durationMonths":6,"scheduleMode":"REGULAR","status":"UPCOMING"}
								""".formatted(courseId)))
				.andExpect(status().isCreated());

		mockMvc.perform(post("/api/v1/batches")
						.header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"courseId":%s,"branchId":1,"batchNumber":"2026/PB02","startDate":"2026-08-01","endDate":"2026-12-31","durationMonths":5,"scheduleMode":"MANUAL","status":"UPCOMING"}
								""".formatted(courseId)))
				.andExpect(status().isConflict());
	}

	@Test
	void activeBranchHeaderFiltersBatches() throws Exception {
		Long cityBranchId = authTestData.branch("IHM-CITY").getId();
		authTestData.user("phase4_super_branch", "SUPER_ADMIN", UserStatus.ACTIVE, "IHM-MAIN");
		String token = login("phase4_super_branch");
		String courseId = createCourse(token);
		createBatch(token, courseId, 1L, "2026/PB02");
		createBatch(token, courseId, cityBranchId, "2026/PB03");

		mockMvc.perform(get("/api/v1/batches")
						.header("Authorization", "Bearer " + token)
						.header("X-Active-Branch-Id", cityBranchId))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.totalElements").value(1))
				.andExpect(jsonPath("$.content[0].batchNumber").value("2026/PB03"));
	}

	@Test
	void feePlanPreviewUsesMonthEndAndFinalRounding() throws Exception {
		authTestData.user("phase4_admin_fee", "ADMIN", UserStatus.ACTIVE);
		String token = login("phase4_admin_fee");
		String courseId = createCourse(token);
		String batchId = createBatch(token, courseId, 1L, "2026/PB02");

		mockMvc.perform(post("/api/v1/batches/%s/fee-plan/installment-preview".formatted(batchId))
						.header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"registrationFee":5000.00,"courseFee":10000.00,"examinationFee":7500.00,"durationMonths":3,"monthlyDueDay":31,"examinationDueDate":"2026-11-15","currencyCode":"LKR"}
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.charges[1].dueDate").value("2026-07-31"))
				.andExpect(jsonPath("$.charges[2].dueDate").value("2026-08-31"))
				.andExpect(jsonPath("$.charges[3].dueDate").value("2026-09-30"))
				.andExpect(jsonPath("$.charges[1].amount").value(3333.33))
				.andExpect(jsonPath("$.charges[3].amount").value(3333.34));

		mockMvc.perform(post("/api/v1/batches/%s/fee-plan".formatted(batchId))
						.header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"registrationFee":5000.00,"courseFee":10000.00,"examinationFee":7500.00,"durationMonths":3,"monthlyDueDay":31,"examinationDueDate":"2026-11-15","currencyCode":"LKR","status":"ACTIVE"}
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.courseFee").value(10000.00));
	}

	@Test
	void lecturerAssignmentRequiresLecturerInBatchBranch() throws Exception {
		Long cityBranchId = authTestData.branch("IHM-CITY").getId();
		authTestData.user("phase4_admin_lecturer", "ADMIN", UserStatus.ACTIVE, "IHM-MAIN");
		authTestData.user("phase4_main_lecturer", "LECTURER", UserStatus.ACTIVE, "IHM-MAIN");
		UserAccount cityLecturer = authTestData.user("phase4_city_lecturer", "LECTURER", UserStatus.ACTIVE, "IHM-CITY");
		String token = login("phase4_admin_lecturer");
		String courseId = createCourse(token);
		String batchId = createBatch(token, courseId, 1L, "2026/PB02");
		String mainLecturerId = userId(token, "phase4_main_lecturer");
		Long cityLecturerId = cityLecturer.getId();

		mockMvc.perform(post("/api/v1/batches/%s/lecturers".formatted(batchId))
						.header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"lecturerUserId":%s,"assignmentStartDate":"2026-07-01","assignmentEndDate":null,"status":"ACTIVE"}
								""".formatted(cityLecturerId)))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("Selected lecturer must be assigned to the batch branch"));

		mockMvc.perform(post("/api/v1/batches/%s/lecturers".formatted(batchId))
						.header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"lecturerUserId":%s,"assignmentStartDate":"2026-07-01","assignmentEndDate":null,"status":"ACTIVE"}
								""".formatted(mainLecturerId)))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.lecturerUsername").value("phase4_main_lecturer"));

		assertThat(cityBranchId).isNotNull();
	}

	@Test
	void lecturerAssignmentSyncAddsRemovesAndDeduplicatesSelections() throws Exception {
		authTestData.user("phase4_admin_sync", "ADMIN", UserStatus.ACTIVE, "IHM-MAIN");
		UserAccount chefLecturer = authTestData.user("phase4_sync_chef", "LECTURER", UserStatus.ACTIVE, "IHM-MAIN");
		UserAccount pastryLecturer = authTestData.user("phase4_sync_pastry", "LECTURER", UserStatus.ACTIVE, "IHM-MAIN");
		String token = login("phase4_admin_sync");
		String courseId = createCourse(token);
		String batchId = createBatch(token, courseId, 1L, "2026/PB02");

		mockMvc.perform(put("/api/v1/batches/%s/lecturers".formatted(batchId))
						.header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"lecturerUserIds":[%s,%s,%s],"assignmentStartDate":"2026-07-01","assignmentEndDate":null}
								""".formatted(chefLecturer.getId(), pastryLecturer.getId(), chefLecturer.getId())))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(2));

		assertThat(activeAssignmentCount(batchId, chefLecturer.getId())).isEqualTo(1);
		assertThat(activeAssignmentCount(batchId, pastryLecturer.getId())).isEqualTo(1);

		mockMvc.perform(put("/api/v1/batches/%s/lecturers".formatted(batchId))
						.header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"lecturerUserIds":[%s,%s],"assignmentStartDate":"2026-07-01","assignmentEndDate":null}
								""".formatted(chefLecturer.getId(), pastryLecturer.getId())))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(2));

		assertThat(activeAssignmentCount(batchId, chefLecturer.getId())).isEqualTo(1);
		assertThat(activeAssignmentCount(batchId, pastryLecturer.getId())).isEqualTo(1);

		mockMvc.perform(put("/api/v1/batches/%s/lecturers".formatted(batchId))
						.header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"lecturerUserIds":[%s],"assignmentStartDate":"2026-07-01","assignmentEndDate":null}
								""".formatted(pastryLecturer.getId())))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(1))
				.andExpect(jsonPath("$[0].lecturerUsername").value("phase4_sync_pastry"));

		assertThat(activeAssignmentCount(batchId, chefLecturer.getId())).isZero();
		assertThat(inactiveAssignmentCount(batchId, chefLecturer.getId())).isEqualTo(1);
		assertThat(activeAssignmentCount(batchId, pastryLecturer.getId())).isEqualTo(1);
		assertThat(auditLogRepository.countByActionAndEntityTypeAndEntityId("BATCH_LECTURER_STATUS_CHANGED", "BatchLecturer",
				assignmentId(batchId, chefLecturer.getId()))).isEqualTo(1);
	}

	@Test
	void lecturerAssignmentRejectsInvalidRoleInactiveAndWrongBranchUsers() throws Exception {
		authTestData.branch("IHM-CITY");
		authTestData.user("phase4_admin_invalid_lecturers", "ADMIN", UserStatus.ACTIVE, "IHM-MAIN");
		UserAccount nonLecturer = authTestData.user("phase4_invalid_admin_user", "ADMIN", UserStatus.ACTIVE, "IHM-MAIN");
		UserAccount inactiveLecturer = authTestData.user("phase4_inactive_lecturer", "LECTURER", UserStatus.DISABLED, "IHM-MAIN");
		UserAccount cityLecturer = authTestData.user("phase4_wrong_branch_lecturer", "LECTURER", UserStatus.ACTIVE, "IHM-CITY");
		String token = login("phase4_admin_invalid_lecturers");
		String courseId = createCourse(token);
		String batchId = createBatch(token, courseId, 1L, "2026/PB02");

		mockMvc.perform(put("/api/v1/batches/%s/lecturers".formatted(batchId))
						.header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"lecturerUserIds":[%s],"assignmentStartDate":"2026-07-01","assignmentEndDate":null}
								""".formatted(nonLecturer.getId())))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("Selected user must have the LECTURER role"));

		mockMvc.perform(put("/api/v1/batches/%s/lecturers".formatted(batchId))
						.header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"lecturerUserIds":[%s],"assignmentStartDate":"2026-07-01","assignmentEndDate":null}
								""".formatted(inactiveLecturer.getId())))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("Selected lecturer must be active"));

		mockMvc.perform(put("/api/v1/batches/%s/lecturers".formatted(batchId))
						.header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"lecturerUserIds":[%s],"assignmentStartDate":"2026-07-01","assignmentEndDate":null}
								""".formatted(cityLecturer.getId())))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("Selected lecturer must be assigned to the batch branch"));
	}

	private String createCourse(String token) throws Exception {
		MvcResult result = mockMvc.perform(post("/api/v1/courses")
						.header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"name":"Pastry & Bakery","shortCode":"PB","status":"ACTIVE"}
								"""))
				.andExpect(status().isCreated())
				.andReturn();
		return extractJsonNumber(result.getResponse().getContentAsString(), "id");
	}

	private String createBatch(String token, String courseId, Long branchId, String batchNumber) throws Exception {
		MvcResult result = mockMvc.perform(post("/api/v1/batches")
						.header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"courseId":%s,"branchId":%s,"batchNumber":"%s","startDate":"2026-07-01","endDate":"2026-12-31","durationMonths":6,"scheduleMode":"REGULAR","status":"UPCOMING"}
								""".formatted(courseId, branchId, batchNumber)))
				.andExpect(status().isCreated())
				.andReturn();
		return extractJsonNumber(result.getResponse().getContentAsString(), "id");
	}

	private String userId(String token, String username) throws Exception {
		MvcResult result = mockMvc.perform(get("/api/v1/users?search=%s".formatted(username))
						.header("Authorization", "Bearer " + token))
				.andExpect(status().isOk())
				.andReturn();
		return extractFirstContentId(result.getResponse().getContentAsString());
	}

	private String login(String username) throws Exception {
		MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"username":"%s","password":"Password123"}
								""".formatted(username)))
				.andExpect(status().isOk())
				.andReturn();
		return extractJsonString(result.getResponse().getContentAsString(), "accessToken");
	}

	private String extractJsonString(String body, String field) {
		String marker = "\"" + field + "\":\"";
		int start = body.indexOf(marker) + marker.length();
		return body.substring(start, body.indexOf('"', start));
	}

	private String extractJsonNumber(String body, String field) {
		String marker = "\"" + field + "\":";
		int start = body.indexOf(marker) + marker.length();
		int comma = body.indexOf(',', start);
		int brace = body.indexOf('}', start);
		int end = comma == -1 ? brace : Math.min(comma, brace);
		return body.substring(start, end);
	}

	private String extractFirstContentId(String body) {
		int contentStart = body.indexOf("\"content\":[");
		return extractJsonNumber(body.substring(contentStart), "id");
	}

	private int activeAssignmentCount(String batchId, Long lecturerUserId) {
		Integer count = jdbcTemplate.queryForObject("""
				select count(*)
				from batch_lecturers
				where batch_id = ? and lecturer_user_id = ? and status = 'ACTIVE'
				""", Integer.class, Long.valueOf(batchId), lecturerUserId);
		return count == null ? 0 : count;
	}

	private int inactiveAssignmentCount(String batchId, Long lecturerUserId) {
		Integer count = jdbcTemplate.queryForObject("""
				select count(*)
				from batch_lecturers
				where batch_id = ? and lecturer_user_id = ? and status = 'INACTIVE'
				""", Integer.class, Long.valueOf(batchId), lecturerUserId);
		return count == null ? 0 : count;
	}

	private String assignmentId(String batchId, Long lecturerUserId) {
		return jdbcTemplate.queryForObject("""
				select id
				from batch_lecturers
				where batch_id = ? and lecturer_user_id = ?
				order by id
				limit 1
				""", String.class, Long.valueOf(batchId), lecturerUserId);
	}
}
