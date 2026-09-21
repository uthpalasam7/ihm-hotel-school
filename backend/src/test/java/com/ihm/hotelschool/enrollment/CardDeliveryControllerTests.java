package com.ihm.hotelschool.enrollment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ihm.hotelschool.auth.AuthTestData;
import com.ihm.hotelschool.delivery.DocumentDeliveryDispatcher;
import com.ihm.hotelschool.user.UserStatus;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

@SpringBootTest(properties={"spring.mail.host=mail.example.invalid","app.delivery.from=school@example.invalid","app.delivery.poll-ms=3600000","management.health.mail.enabled=false"})
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CardDeliveryControllerTests {
    @Autowired MockMvc mvc;
    @Autowired AuthTestData data;
    @Autowired ObjectMapper json;
    @Autowired JdbcTemplate db;
    @Autowired DocumentDeliveryDispatcher dispatcher;
    @MockitoBean JavaMailSender sender;
    long admin, student, batch;

    @BeforeEach void setup() throws Exception {
        cleanup();
        when(sender.createMimeMessage()).thenAnswer(invocation -> new MimeMessage(Session.getInstance(new Properties())));
        admin=data.user("delivery_admin","ADMIN",UserStatus.ACTIVE).getId();
        student=body(postAs("/students",Map.of("fullName","Sample Student","nic","200012345678",
                "contactNumber","0771234567","email","student@example.invalid","address","Colombo"),admin)
                .andExpect(status().isCreated())).path("id").asLong();
        long course=body(postAs("/courses",Map.of("name","Pastry","shortCode","PB","status","ACTIVE"),admin)
                .andExpect(status().isCreated())).path("id").asLong();
        batch=body(postAs("/batches",Map.of("courseId",course,"branchId",1,"batchNumber","2026/PB01",
                "startDate","2026-01-20","endDate","2026-03-31","durationMonths",3,"scheduleMode","MANUAL","status","UPCOMING"),admin)
                .andExpect(status().isCreated())).path("id").asLong();
        mvc.perform(put("/api/v1/batches/"+batch+"/fee-plan").with(jwt().jwt(j->j.claim("userId",admin)))
                .contentType(MediaType.APPLICATION_JSON).content("{\"registrationFee\":100,\"courseFee\":1000,\"examinationFee\":500,\"durationMonths\":3,\"monthlyDueDay\":10,\"examinationDueDate\":\"2026-03-31\",\"currencyCode\":\"LKR\",\"status\":\"ACTIVE\"}"))
                .andExpect(status().isOk());
        postAs("/enrollments",Map.of("studentId",student,"batchId",batch,"enrollmentDate","2026-01-15"),admin)
                .andExpect(status().isCreated());
        postAs("/students/"+student+"/card",Map.of(),admin).andExpect(status().isOk());
    }
    @AfterEach void cleanup() {
        for (String table:new String[]{"document_delivery_attempts","document_deliveries","student_card_events","student_cards",
                "student_charges","enrollments","batch_lecturers","fee_plans","course_batches","courses",
                "refresh_tokens","audit_logs","students","user_roles","user_branches","users"}) db.update("delete from "+table);
        db.update("delete from branches where id <> 1"); db.update("update branches set status='ACTIVE' where id=1");
    }
    @Test void emailIsQueuedOnceAndSentFromSavedAddressAfterCommit() throws Exception {
        mvc.perform(get("/api/v1/document-deliveries/availability").with(jwt().jwt(j->j.claim("userId",admin))))
                .andExpect(jsonPath("$.available").value(true));
        String key=UUID.randomUUID().toString();
        long first=body(postAs("/students/"+student+"/card/email",Map.of("idempotencyKey",key),admin)
                .andExpect(status().isAccepted()).andExpect(jsonPath("$.status").value("QUEUED"))).path("id").asLong();
        long second=body(postAs("/students/"+student+"/card/email",Map.of("idempotencyKey",key),admin)
                .andExpect(status().isAccepted())).path("id").asLong();
        assertThat(second).isEqualTo(first);
        assertThat(db.queryForObject("select count(*) from document_deliveries",Long.class)).isEqualTo(1);
        db.update("update students set email='changed@example.invalid' where id=?",student);
        dispatcher.dispatchOne();
        var captor=org.mockito.ArgumentCaptor.forClass(MimeMessage.class);
        verify(sender,times(1)).send(captor.capture());
        assertThat(captor.getValue().getAllRecipients()[0].toString()).isEqualTo("student@example.invalid");
        assertThat(db.queryForObject("select status from document_deliveries where id=?",String.class,first)).isEqualTo("ACCEPTED");
        assertThat(db.queryForObject("select count(*) from document_delivery_attempts where delivery_id=?",Long.class,first)).isEqualTo(1);
        mvc.perform(get("/api/v1/students/"+student+"/card/deliveries").with(jwt().jwt(j->j.claim("userId",admin))))
                .andExpect(jsonPath("$.content[0].status").value("ACCEPTED"));
    }
    @Test void revokedCardIsNeverSentAndLecturerCannotQueue() throws Exception {
        long lecturer=data.user("delivery_lecturer","LECTURER",UserStatus.ACTIVE).getId();
        postAs("/students/"+student+"/card/email",Map.of("idempotencyKey",UUID.randomUUID().toString()),lecturer)
                .andExpect(status().isForbidden());
        long id=body(postAs("/students/"+student+"/card/email",Map.of("idempotencyKey",UUID.randomUUID().toString()),admin)
                .andExpect(status().isAccepted())).path("id").asLong();
        postAs("/students/"+student+"/card/revoke",Map.of("reason","Lost"),admin).andExpect(status().isOk());
        dispatcher.dispatchOne();
        verify(sender,never()).send(any(MimeMessage.class));
        assertThat(db.queryForObject("select status from document_deliveries where id=?",String.class,id)).isEqualTo("FAILED");
    }
    @Test void mailFailuresRetryAtMostThreeTimes() throws Exception {
        doThrow(new org.springframework.mail.MailSendException("Temporary failure"))
                .when(sender).send(any(MimeMessage.class));
        long id=body(postAs("/students/"+student+"/card/email",Map.of("idempotencyKey",UUID.randomUUID().toString()),admin)
                .andExpect(status().isAccepted())).path("id").asLong();
        for (int number=1; number<=3; number++) {
            dispatcher.dispatchOne();
            assertThat(db.queryForObject("select attempts from document_deliveries where id=?",Integer.class,id)).isEqualTo(number);
            if (number<3) db.update("update document_deliveries set next_attempt_at=? where id=?",
                    java.sql.Timestamp.from(java.time.Instant.now().minusSeconds(1)),id);
        }
        assertThat(db.queryForObject("select status from document_deliveries where id=?",String.class,id)).isEqualTo("FAILED");
        assertThat(db.queryForObject("select count(*) from document_delivery_attempts where delivery_id=?",Long.class,id)).isEqualTo(3);
        verify(sender,times(3)).send(any(MimeMessage.class));
    }

    @Test void interruptedSendIsNotAutomaticallyRetried() throws Exception {
        doThrow(new AssertionError("Simulated process interruption"))
                .when(sender).send(any(MimeMessage.class));
        long id=body(postAs("/students/"+student+"/card/email",Map.of("idempotencyKey",UUID.randomUUID().toString()),admin)
                .andExpect(status().isAccepted())).path("id").asLong();
        assertThatThrownBy(dispatcher::dispatchOne).isInstanceOf(AssertionError.class);
        assertThat(db.queryForObject("select status from document_deliveries where id=?",String.class,id)).isEqualTo("SENDING");
        db.update("update document_deliveries set updated_at=? where id=?",
                java.sql.Timestamp.from(java.time.Instant.now().minusSeconds(700)),id);
        dispatcher.dispatchOne();
        assertThat(db.queryForObject("select status from document_deliveries where id=?",String.class,id)).isEqualTo("FAILED");
        verify(sender,times(1)).send(any(MimeMessage.class));
    }

    private ResultActions postAs(String path,Object body,long actor) throws Exception {
        return mvc.perform(post("/api/v1"+path).with(jwt().jwt(j->j.claim("userId",actor)))
                .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(body)));
    }
    private JsonNode body(ResultActions result) throws Exception { return json.readTree(result.andReturn().getResponse().getContentAsString()); }
}
