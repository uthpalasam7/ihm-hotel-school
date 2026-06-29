package com.ihm.hotelschool.auth;

import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.blankOrNullString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
class AuthControllerTests {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private AuthTestData authTestData;

	@Test
	void validCredentialsReturnTokensAndCurrentUser() throws Exception {
		authTestData.user("admin_login", "ADMIN", UserStatus.ACTIVE);

		mockMvc.perform(post("/api/v1/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"username":"admin_login","password":"Password123"}
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.accessToken", not(blankOrNullString())))
				.andExpect(jsonPath("$.refreshToken", not(blankOrNullString())))
				.andExpect(jsonPath("$.user.roles[0]").value("ADMIN"))
				.andExpect(jsonPath("$.user.branches[0].code").value("IHM-MAIN"));
	}

	@Test
	void invalidLoginUsesGenericError() throws Exception {
		mockMvc.perform(post("/api/v1/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"username":"missing","password":"wrong"}
								"""))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("AUTHENTICATION_FAILED"))
				.andExpect(jsonPath("$.message").value("Invalid username or password"));
	}

	@Test
	void disabledUserCannotLogin() throws Exception {
		authTestData.user("disabled_login", "ADMIN", UserStatus.DISABLED);

		mockMvc.perform(post("/api/v1/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"username":"disabled_login","password":"Password123"}
					"""))
					.andExpect(status().isUnauthorized());
	}

	@Test
	void repeatedFailedLoginsAreRateLimited() throws Exception {
		authTestData.user("limited_login", "ADMIN", UserStatus.ACTIVE);

		for (int attempt = 1; attempt < 5; attempt++) {
			mockMvc.perform(post("/api/v1/auth/login")
							.contentType(MediaType.APPLICATION_JSON)
							.content("""
									{"username":"limited_login","password":"wrong"}
									"""))
					.andExpect(status().isUnauthorized());
		}

		mockMvc.perform(post("/api/v1/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"username":"limited_login","password":"wrong"}
								"""))
				.andExpect(status().isTooManyRequests())
				.andExpect(jsonPath("$.code").value("RATE_LIMITED"));
	}

	@Test
	void refreshRotatesTokens() throws Exception {
		authTestData.user("refresh_user", "ADMIN", UserStatus.ACTIVE);
		String refreshToken = login("refresh_user").refreshToken();

		mockMvc.perform(post("/api/v1/auth/refresh")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"refreshToken":"%s"}
								""".formatted(refreshToken)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.accessToken", not(blankOrNullString())))
				.andExpect(jsonPath("$.refreshToken", not(blankOrNullString())));

		mockMvc.perform(post("/api/v1/auth/refresh")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"refreshToken":"%s"}
								""".formatted(refreshToken)))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void refreshRejectsInvalidTokenWithAuthenticationError() throws Exception {
		mockMvc.perform(post("/api/v1/auth/refresh")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"refreshToken":"not-a-valid-refresh-token"}
								"""))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("AUTHENTICATION_FAILED"));
	}

	@Test
	void authenticatedUserCanLoadMe() throws Exception {
		authTestData.user("me_user", "LECTURER", UserStatus.ACTIVE);
		LoginTokens tokens = login("me_user");

		mockMvc.perform(get("/api/v1/auth/me")
						.header("Authorization", "Bearer " + tokens.accessToken()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.username").value("me_user"))
				.andExpect(jsonPath("$.roles[0]").value("LECTURER"));
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
		return new LoginTokens(
				extractJsonString(body, "accessToken"),
				extractJsonString(body, "refreshToken"));
	}

	private String extractJsonString(String body, String field) {
		String marker = "\"" + field + "\":\"";
		int start = body.indexOf(marker) + marker.length();
		int end = body.indexOf('"', start);
		return body.substring(start, end);
	}

	private record LoginTokens(String accessToken, String refreshToken) {
	}
}
