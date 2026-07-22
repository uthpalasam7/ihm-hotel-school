package com.ihm.hotelschool.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.ihm.hotelschool.audit.AuditLogRepository;
import com.ihm.hotelschool.auth.AuthTestData;
import com.ihm.hotelschool.user.UserStatus;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.mock.web.MockMultipartFile;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@Sql(statements = {
		"delete from refresh_tokens",
		"delete from audit_logs",
		"delete from students",
		"delete from batch_lecturers",
		"delete from fee_plans",
		"delete from course_batches",
		"delete from courses",
		"delete from user_roles",
		"delete from user_branches",
		"delete from users",
		"delete from branches where id <> 1",
		"update branches set status = 'ACTIVE' where id = 1"
}, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class StudentControllerTests {

	@Autowired private MockMvc mockMvc;
	@Autowired private AuthTestData authTestData;
	@Autowired private AuditLogRepository auditLogRepository;
	@Autowired private JdbcTemplate jdbcTemplate;

	@Test
	void migrationEnforcesRequiredUniqueNicAndStatusConstraints() {
		var actor = authTestData.user("phase5_constraint_admin", "ADMIN", UserStatus.ACTIVE, "IHM-MAIN");
		String insert = """
				insert into students
				(full_name, nic, normalized_nic, contact_number, address, status,
				 created_at, created_by, updated_at, updated_by, version)
				values (?, ?, ?, ?, ?, ?, current_timestamp, ?, current_timestamp, ?, 0)
				""";
		jdbcTemplate.update(insert, "Nimal Perera", "200012345678", "200012345678", "0712345678",
				"Kurunegala", "ACTIVE", actor.getId(), actor.getId());

		assertThatThrownBy(() -> jdbcTemplate.update(insert, "Duplicate Student", "2000 12345678",
				"200012345678", "0770000000", "Colombo", "ACTIVE", actor.getId(), actor.getId()))
				.isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
		assertThatThrownBy(() -> jdbcTemplate.update(insert, null, "199912345678", "199912345678",
				"0770000001", "Colombo", "ACTIVE", actor.getId(), actor.getId()))
				.isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
		assertThatThrownBy(() -> jdbcTemplate.update(insert, "Invalid Status", "199812345678", "199812345678",
				"0770000002", "Colombo", "ARCHIVED", actor.getId(), actor.getId()))
				.isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
	}

	@Test
	void adminCanCreateNormalizeFindSearchUpdateAndDeactivateStudent() throws Exception {
		authTestData.user("phase5_student_admin", "ADMIN", UserStatus.ACTIVE, "IHM-MAIN");
		String token = login("phase5_student_admin");

		MvcResult created = mockMvc.perform(post("/api/v1/students")
					.header("Authorization", "Bearer " + token)
					.header("X-Active-Branch-Id", "1")
					.contentType(MediaType.APPLICATION_JSON)
					.content(studentJson("200012345v", "0712345678")))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.nic").value("200012345V"))
				.andExpect(jsonPath("$.status").value("ACTIVE"))
				.andExpect(jsonPath("$.photoAvailable").value(false))
				.andReturn();
		String studentId = extractJsonNumber(created.getResponse().getContentAsString(), "id");

		mockMvc.perform(post("/api/v1/students")
					.header("Authorization", "Bearer " + token)
					.contentType(MediaType.APPLICATION_JSON)
					.content(studentJson("200012345V", "0710000000")))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.message").value("A student with this NIC already exists"));

		mockMvc.perform(get("/api/v1/students/by-nic/200012345v")
					.header("Authorization", "Bearer " + token))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(Long.valueOf(studentId)));

		mockMvc.perform(get("/api/v1/students?search=071234&page=0&size=10")
					.header("Authorization", "Bearer " + token))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.totalElements").value(1))
				.andExpect(jsonPath("$.content[0].fullName").value("Nimal Perera"));

		mockMvc.perform(put("/api/v1/students/" + studentId)
					.header("Authorization", "Bearer " + token)
					.contentType(MediaType.APPLICATION_JSON)
					.content(studentJson("200012345v", "0777654321")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.contactNumber").value("0777654321"));

		mockMvc.perform(patch("/api/v1/students/" + studentId + "/status")
					.header("Authorization", "Bearer " + token)
					.header("X-Active-Branch-Id", "1")
					.contentType(MediaType.APPLICATION_JSON)
					.content("{\"status\":\"INACTIVE\",\"reason\":\"Record paused\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("INACTIVE"));

		assertThat(auditLogRepository.countByActionAndEntityTypeAndEntityId("STUDENT_CREATED", "Student", studentId)).isEqualTo(1);
		assertThat(auditLogRepository.countByActionAndEntityTypeAndEntityId("STUDENT_UPDATED", "Student", studentId)).isEqualTo(1);
		assertThat(auditLogRepository.countByActionAndEntityTypeAndEntityId("STUDENT_STATUS_CHANGED", "Student", studentId)).isEqualTo(1);
	}

	@Test
	void validationRoleAndActiveBranchSecurityAreEnforced() throws Exception {
		authTestData.branch("IHM-CITY");
		authTestData.user("phase5_limited_admin", "ADMIN", UserStatus.ACTIVE, "IHM-MAIN");
		authTestData.user("phase5_student_lecturer", "LECTURER", UserStatus.ACTIVE, "IHM-MAIN");
		String adminToken = login("phase5_limited_admin");
		String lecturerToken = login("phase5_student_lecturer");

		mockMvc.perform(post("/api/v1/students")
					.header("Authorization", "Bearer " + adminToken)
					.contentType(MediaType.APPLICATION_JSON)
					.content("{\"fullName\":\"\",\"nic\":\"\",\"contactNumber\":\"\",\"address\":\"\"}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));

		mockMvc.perform(post("/api/v1/students")
					.header("Authorization", "Bearer " + adminToken)
					.contentType(MediaType.APPLICATION_JSON)
					.content("""
							{"fullName":"Invalid Student","nic":"2000-12345v","contactNumber":"071234",
							"alternativeContactNumber":"077-1234567","email":null,"address":"Kurunegala",
							"dateOfBirth":null,"gender":"Unknown","remarks":null}
							"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.fieldErrors[?(@.field == 'nic')]").exists())
				.andExpect(jsonPath("$.fieldErrors[?(@.field == 'contactNumber')]").exists())
				.andExpect(jsonPath("$.fieldErrors[?(@.field == 'alternativeContactNumber')]").exists())
				.andExpect(jsonPath("$.fieldErrors[?(@.field == 'gender')]").exists());

		mockMvc.perform(get("/api/v1/students")
					.header("Authorization", "Bearer " + lecturerToken))
				.andExpect(status().isForbidden());

		mockMvc.perform(get("/api/v1/students")
					.header("Authorization", "Bearer " + adminToken)
					.header("X-Active-Branch-Id", "2"))
				.andExpect(status().isForbidden());
	}

	@Test
	void photoUploadCreatesSecureFullAndThumbnailVariantsAndSupportsReplacementAndDeletion() throws Exception {
		authTestData.user("phase5_photo_admin", "SUPER_ADMIN", UserStatus.ACTIVE, "IHM-MAIN");
		String token = login("phase5_photo_admin");
		String studentId = createStudent(token, "199912345678");
		byte[] png = image("png", 500, 250, Color.ORANGE);

		mockMvc.perform(multipart("/api/v1/students/" + studentId + "/photo")
					.file(new MockMultipartFile("photo", "student.png", "image/png", png))
					.header("Authorization", "Bearer " + token))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.photoAvailable").value(true))
				.andExpect(jsonPath("$.photoThumbnailUrl").value("/api/v1/students/" + studentId + "/photo?variant=thumbnail"));

		MvcResult thumbnail = mockMvc.perform(get("/api/v1/students/" + studentId + "/photo?variant=thumbnail")
					.header("Authorization", "Bearer " + token))
				.andExpect(status().isOk())
				.andExpect(content().contentType("image/png"))
				.andExpect(header().string("X-Content-Type-Options", "nosniff"))
				.andReturn();
		BufferedImage thumbnailImage = ImageIO.read(new java.io.ByteArrayInputStream(thumbnail.getResponse().getContentAsByteArray()));
		assertThat(Math.max(thumbnailImage.getWidth(), thumbnailImage.getHeight())).isEqualTo(96);

		byte[] jpeg = image("jpg", 320, 640, Color.BLUE);
		mockMvc.perform(multipart("/api/v1/students/" + studentId + "/photo")
					.file(new MockMultipartFile("photo", "replacement.jpg", "image/jpeg", jpeg))
					.header("Authorization", "Bearer " + token))
				.andExpect(status().isOk());

		mockMvc.perform(multipart("/api/v1/students/" + studentId + "/photo")
					.file(new MockMultipartFile("photo", "fake.png", "image/png", "not-an-image".getBytes()))
					.header("Authorization", "Bearer " + token))
				.andExpect(status().isBadRequest());

		mockMvc.perform(multipart("/api/v1/students/" + studentId + "/photo")
					.file(new MockMultipartFile("photo", "mismatch.jpg", "image/jpeg", png))
					.header("Authorization", "Bearer " + token))
				.andExpect(status().isBadRequest());

		mockMvc.perform(multipart("/api/v1/students/" + studentId + "/photo")
					.file(new MockMultipartFile("photo", "large.png", "image/png", new byte[(5 * 1024 * 1024) + 1]))
					.header("Authorization", "Bearer " + token))
				.andExpect(status().isPayloadTooLarge())
				.andExpect(jsonPath("$.code").value("FILE_TOO_LARGE"));

		mockMvc.perform(multipart("/api/v1/students/" + studentId + "/photo")
					.file(new MockMultipartFile("photo", "wide.png", "image/png", image("png", 10001, 1, Color.GREEN)))
					.header("Authorization", "Bearer " + token))
				.andExpect(status().isBadRequest());

		mockMvc.perform(delete("/api/v1/students/" + studentId + "/photo")
					.header("Authorization", "Bearer " + token))
				.andExpect(status().isNoContent());

		mockMvc.perform(get("/api/v1/students/" + studentId + "/photo")
					.header("Authorization", "Bearer " + token))
				.andExpect(status().isNotFound());
		assertThat(auditLogRepository.countByActionAndEntityTypeAndEntityId("STUDENT_PHOTO_UPDATED", "Student", studentId)).isEqualTo(2);
		assertThat(auditLogRepository.countByActionAndEntityTypeAndEntityId("STUDENT_PHOTO_REMOVED", "Student", studentId)).isEqualTo(1);
	}

	private String createStudent(String token, String nic) throws Exception {
		MvcResult result = mockMvc.perform(post("/api/v1/students")
					.header("Authorization", "Bearer " + token)
					.contentType(MediaType.APPLICATION_JSON)
					.content(studentJson(nic, "0712345678")))
				.andExpect(status().isCreated())
				.andReturn();
		return extractJsonNumber(result.getResponse().getContentAsString(), "id");
	}

	private String studentJson(String nic, String contact) {
		return """
				{\"fullName\":\"Nimal Perera\",\"nic\":\"%s\",\"contactNumber\":\"%s\",\"alternativeContactNumber\":null,\"email\":\"nimal@example.invalid\",\"address\":\"Kurunegala\",\"dateOfBirth\":null,\"gender\":null,\"remarks\":null}
				""".formatted(nic, contact);
	}

	private byte[] image(String format, int width, int height, Color color) throws Exception {
		BufferedImage image = new BufferedImage(width, height, format.equals("png") ? BufferedImage.TYPE_INT_ARGB : BufferedImage.TYPE_INT_RGB);
		var graphics = image.createGraphics();
		graphics.setColor(color);
		graphics.fillRect(0, 0, width, height);
		graphics.dispose();
		ByteArrayOutputStream output = new ByteArrayOutputStream();
		ImageIO.write(image, format, output);
		return output.toByteArray();
	}

	private String login(String username) throws Exception {
		MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
					.contentType(MediaType.APPLICATION_JSON)
					.content("{\"username\":\"%s\",\"password\":\"Password123\"}".formatted(username)))
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
}
