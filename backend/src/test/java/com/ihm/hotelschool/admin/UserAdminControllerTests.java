package com.ihm.hotelschool.admin;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.ihm.hotelschool.audit.AuditLogRepository;
import com.ihm.hotelschool.auth.AuthTestData;
import com.ihm.hotelschool.user.UserStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class UserAdminControllerTests {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private AuthTestData authTestData;

	@Autowired
	private AuditLogRepository auditLogRepository;

	@Test
	void superAdminCanCreateAdminUser() throws Exception {
		authTestData.user("user_super_create", "SUPER_ADMIN", UserStatus.ACTIVE);
		String token = login("user_super_create").accessToken();

		MvcResult result = mockMvc.perform(post("/api/v1/users")
						.header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "username":"created_admin",
								  "email":"created_admin@example.invalid",
								  "fullName":"Created Admin",
								  "contactNumber":"0711111111",
								  "status":"ACTIVE",
								  "roleCodes":["ADMIN"],
								  "branchIds":[1],
								  "temporaryPassword":"TempPass123"
								}
								"""))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.status").value("PASSWORD_CHANGE_REQUIRED"))
				.andExpect(jsonPath("$.roles[0].code").value("ADMIN"))
				.andExpect(jsonPath("$.temporaryPassword").value("TempPass123"))
				.andReturn();

		String userId = extractJsonNumber(result.getResponse().getContentAsString(), "id");
		org.assertj.core.api.Assertions.assertThat(auditLogRepository.countByActionAndEntityTypeAndEntityId("USER_CREATED", "User", userId))
				.isEqualTo(1);
	}

	@Test
	void adminCanCreateLecturerOnlyInAssignedBranches() throws Exception {
		authTestData.user("user_admin_create", "ADMIN", UserStatus.ACTIVE);
		String token = login("user_admin_create").accessToken();

		mockMvc.perform(post("/api/v1/users")
						.header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "username":"created_lecturer",
								  "email":"created_lecturer@example.invalid",
								  "fullName":"Created Lecturer",
								  "status":"ACTIVE",
								  "roleCodes":["LECTURER"],
								  "branchIds":[1],
								  "temporaryPassword":"TempPass123"
								}
								"""))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.roles[0].code").value("LECTURER"));
	}

	@Test
	void adminCannotCreateAdminUser() throws Exception {
		authTestData.user("user_admin_forbidden", "ADMIN", UserStatus.ACTIVE);
		String token = login("user_admin_forbidden").accessToken();

		mockMvc.perform(post("/api/v1/users")
						.header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "username":"blocked_admin",
								  "fullName":"Blocked Admin",
								  "status":"ACTIVE",
								  "roleCodes":["ADMIN"],
								  "branchIds":[1],
								  "temporaryPassword":"TempPass123"
								}
								"""))
				.andExpect(status().isForbidden());
	}

	@Test
	void lecturerCannotAccessUserAdmin() throws Exception {
		authTestData.user("user_lecturer_forbidden", "LECTURER", UserStatus.ACTIVE);
		String token = login("user_lecturer_forbidden").accessToken();

		mockMvc.perform(get("/api/v1/users")
						.header("Authorization", "Bearer " + token))
				.andExpect(status().isForbidden());
	}

	@Test
	void activeBranchHeaderFiltersUserListAndKeepsAllAssignedBranchesInResponse() throws Exception {
		authTestData.branch("IHM-CITY");
		authTestData.user("user_multi_admin", "ADMIN", UserStatus.ACTIVE, "IHM-MAIN", "IHM-CITY");
		authTestData.user("user_city_lecturer", "LECTURER", UserStatus.ACTIVE, "IHM-MAIN", "IHM-CITY");
		String token = login("user_multi_admin").accessToken();

		mockMvc.perform(get("/api/v1/users?search=user_city_lecturer")
						.header("Authorization", "Bearer " + token)
						.header("X-Active-Branch-Id", "1"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content[0].branches.length()").value(2))
				.andExpect(jsonPath("$.content[0].branches[0].code").value("IHM-CITY"))
				.andExpect(jsonPath("$.content[0].branches[1].code").value("IHM-MAIN"));
	}

	@Test
	void unauthorizedActiveBranchHeaderIsRejected() throws Exception {
		Long cityBranchId = authTestData.branch("IHM-CITY").getId();
		authTestData.user("user_main_admin", "ADMIN", UserStatus.ACTIVE, "IHM-MAIN");
		String token = login("user_main_admin").accessToken();

		mockMvc.perform(get("/api/v1/users")
						.header("Authorization", "Bearer " + token)
						.header("X-Active-Branch-Id", cityBranchId))
				.andExpect(status().isForbidden());
	}

	@Test
	void disablingUserRevokesRefreshTokensAndAudits() throws Exception {
		authTestData.user("user_super_disable", "SUPER_ADMIN", UserStatus.ACTIVE);
		authTestData.user("user_to_disable", "LECTURER", UserStatus.ACTIVE);
		LoginTokens superTokens = login("user_super_disable");
		LoginTokens targetTokens = login("user_to_disable");

		MvcResult userResult = mockMvc.perform(get("/api/v1/users?search=user_to_disable")
						.header("Authorization", "Bearer " + superTokens.accessToken()))
				.andExpect(status().isOk())
				.andReturn();
		String userId = extractFirstContentId(userResult.getResponse().getContentAsString());

		mockMvc.perform(patch("/api/v1/users/%s/status".formatted(userId))
						.header("Authorization", "Bearer " + superTokens.accessToken())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"status":"DISABLED","reason":"left school"}
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("DISABLED"));

		mockMvc.perform(post("/api/v1/auth/refresh")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"refreshToken":"%s"}
								""".formatted(targetTokens.refreshToken())))
				.andExpect(status().isUnauthorized());
		org.assertj.core.api.Assertions.assertThat(auditLogRepository.countByActionAndEntityTypeAndEntityId("USER_STATUS_CHANGED", "User", userId))
				.isEqualTo(1);
	}

	@Test
	void resetPasswordRequiresPasswordChangeAndRevokesRefreshTokens() throws Exception {
		authTestData.user("user_super_reset", "SUPER_ADMIN", UserStatus.ACTIVE);
		authTestData.user("user_to_reset", "LECTURER", UserStatus.ACTIVE);
		LoginTokens superTokens = login("user_super_reset");
		LoginTokens targetTokens = login("user_to_reset");

		MvcResult userResult = mockMvc.perform(get("/api/v1/users?search=user_to_reset")
						.header("Authorization", "Bearer " + superTokens.accessToken()))
				.andExpect(status().isOk())
				.andReturn();
		String userId = extractFirstContentId(userResult.getResponse().getContentAsString());

		mockMvc.perform(post("/api/v1/users/%s/reset-password".formatted(userId))
						.header("Authorization", "Bearer " + superTokens.accessToken())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"temporaryPassword":"NewTemp123","reason":"forgotten"}
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("PASSWORD_CHANGE_REQUIRED"))
				.andExpect(jsonPath("$.temporaryPassword").value("NewTemp123"));

		mockMvc.perform(post("/api/v1/auth/refresh")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"refreshToken":"%s"}
								""".formatted(targetTokens.refreshToken())))
				.andExpect(status().isUnauthorized());
	}

	private LoginTokens login(String username) throws Exception {
		MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"username":"%s","password":"Password123"}
								""".formatted(username)))
				.andExpect(status().isOk())
				.andReturn();
		String body = result.getResponse().getContentAsString();
		return new LoginTokens(extractJsonString(body, "accessToken"), extractJsonString(body, "refreshToken"));
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

	private String extractFirstContentId(String body) {
		int contentStart = body.indexOf("\"content\":[");
		return extractJsonNumber(body.substring(contentStart), "id");
	}

	private record LoginTokens(String accessToken, String refreshToken) {
	}
}
