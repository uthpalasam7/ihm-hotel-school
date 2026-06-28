package com.ihm.hotelschool.admin;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.ihm.hotelschool.audit.AuditLogRepository;
import com.ihm.hotelschool.user.UserStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Sql(statements = {
		"delete from refresh_tokens",
		"delete from audit_logs",
		"delete from user_roles",
		"delete from user_branches",
		"delete from users",
		"delete from branches where id <> 1",
		"update branches set status = 'ACTIVE' where id = 1"
}, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class BranchAdminControllerTests {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private com.ihm.hotelschool.auth.AuthTestData authTestData;

	@Autowired
	private AuditLogRepository auditLogRepository;

	@Test
	void superAdminCanCreateBranchAndAuditIsRecorded() throws Exception {
		authTestData.user("branch_super", "SUPER_ADMIN", UserStatus.ACTIVE);
		String token = login("branch_super");

		MvcResult result = mockMvc.perform(post("/api/v1/branches")
						.header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"code":"ihm-city","name":"IHM City","address":"Colombo","contactNumber":"0111111111","status":"ACTIVE"}
								"""))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.code").value("IHM-CITY"))
				.andReturn();

		String branchId = extractJsonNumber(result.getResponse().getContentAsString(), "id");
		org.assertj.core.api.Assertions.assertThat(auditLogRepository.countByActionAndEntityTypeAndEntityId("BRANCH_CREATED", "Branch", branchId))
				.isEqualTo(1);
	}

	@Test
	void adminCannotCreateBranch() throws Exception {
		authTestData.user("branch_admin_forbidden", "ADMIN", UserStatus.ACTIVE);
		String token = login("branch_admin_forbidden");

		mockMvc.perform(post("/api/v1/branches")
						.header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"code":"IHM-NORTH","name":"IHM North","status":"ACTIVE"}
								"""))
				.andExpect(status().isForbidden());
	}

	@Test
	void duplicateBranchCodeIsRejected() throws Exception {
		authTestData.user("branch_duplicate_super", "SUPER_ADMIN", UserStatus.ACTIVE);
		String token = login("branch_duplicate_super");

		mockMvc.perform(post("/api/v1/branches")
						.header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"code":"IHM-MAIN","name":"Duplicate Main","status":"ACTIVE"}
								"""))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.code").value("CONFLICT"));
	}

	@Test
	void onlyActiveDefaultBranchCannotBeDeactivated() throws Exception {
		authTestData.user("branch_status_super", "SUPER_ADMIN", UserStatus.ACTIVE);
		String token = login("branch_status_super");

		mockMvc.perform(patch("/api/v1/branches/1/status")
						.header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"status":"INACTIVE","reason":"closing"}
								"""))
				.andExpect(status().isConflict());
	}

	@Test
	void lecturerCanListOnlyAssignedBranches() throws Exception {
		authTestData.user("branch_lecturer", "LECTURER", UserStatus.ACTIVE);
		String token = login("branch_lecturer");

		mockMvc.perform(get("/api/v1/branches")
						.header("Authorization", "Bearer " + token))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content[0].code").value("IHM-MAIN"));
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
		int end = body.indexOf(',', start);
		return body.substring(start, end);
	}
}
