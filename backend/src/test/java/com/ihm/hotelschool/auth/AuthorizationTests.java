package com.ihm.hotelschool.auth;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthorizationTests {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private AuthTestData authTestData;

	@Test
	void unauthenticatedRequestsAreRejected() throws Exception {
		mockMvc.perform(get("/api/v1/auth/me"))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void roleRestrictionsAreEnforced() throws Exception {
		authTestData.user("lecturer_only", "LECTURER", UserStatus.ACTIVE);
		String token = login("lecturer_only");

		mockMvc.perform(get("/api/v1/auth-check/admin")
						.header("Authorization", "Bearer " + token))
				.andExpect(status().isForbidden());

		mockMvc.perform(get("/api/v1/auth-check/lecturer")
						.header("Authorization", "Bearer " + token))
				.andExpect(status().isOk());
	}

	@Test
	void branchRestrictionsAreEnforced() throws Exception {
		authTestData.user("branch_admin", "ADMIN", UserStatus.ACTIVE);
		String token = login("branch_admin");

		mockMvc.perform(get("/api/v1/auth-check/branches/1")
						.header("Authorization", "Bearer " + token))
				.andExpect(status().isOk());

		mockMvc.perform(get("/api/v1/auth-check/branches/999")
						.header("Authorization", "Bearer " + token))
				.andExpect(status().isForbidden());
	}

	private String login(String username) throws Exception {
		MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"username":"%s","password":"Password123"}
								""".formatted(username)))
				.andExpect(status().isOk())
				.andReturn();
		String body = result.getResponse().getContentAsString();
		String marker = "\"accessToken\":\"";
		int start = body.indexOf(marker) + marker.length();
		return body.substring(start, body.indexOf('"', start));
	}
}
