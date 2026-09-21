package com.ihm.hotelschool.enrollment;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ihm.hotelschool.auth.AuthTestData;
import com.ihm.hotelschool.user.UserStatus;
import java.util.Map;
import java.util.concurrent.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class EnrollmentControllerTests {
    @Autowired MockMvc mvc;
    @Autowired AuthTestData data;
    @Autowired ObjectMapper json;
    @Autowired JdbcTemplate db;
    long admin, student, batch;

    @BeforeEach void setup() throws Exception {
        cleanup();
        admin = data.user("enrollment_admin", "ADMIN", UserStatus.ACTIVE).getId();
        student = student("200012345678");
        batch = batch("2026/PB01");
    }

    @AfterEach void cleanup() {
        for (String table : new String[]{"student_card_events", "student_cards", "student_charges", "enrollments", "batch_lecturers", "fee_plans", "course_batches", "courses",
                "refresh_tokens", "audit_logs", "students", "user_roles", "user_branches", "users"}) db.update("delete from " + table);
        db.update("delete from branches where id <> 1");
        db.update("update branches set status = 'ACTIVE' where id = 1");
    }

    @Test void enrollmentCreatesSnapshotChargesAndAuditWithoutChangingPreview() throws Exception {
        postAs("/enrollments/preview", request(student, batch), admin).andExpect(status().isOk())
                .andExpect(jsonPath("$.totalAmount").value(1600))
                .andExpect(jsonPath("$.charges.length()").value(5))
                .andExpect(jsonPath("$.charges[0].dueDate").value("2026-01-15"))
                .andExpect(jsonPath("$.charges[2].dueDate").value("2026-02-28"))
                .andExpect(jsonPath("$.charges[3].amount").value(333.34));
        assertThat(count("enrollments")).isZero();
        long id = create(student, batch).path("id").asLong();
        assertThat(db.queryForObject("select registration_number from enrollments where id=?", String.class, id)).isEqualTo("2026/PB01/0001");
        assertThat(count("student_charges")).isEqualTo(5);
        getAs("/batches/"+batch, admin).andExpect(jsonPath("$.studentCount").value(1));
        assertThat(db.queryForObject("select count(*) from audit_logs where action='ENROLLMENT_CREATED'", Long.class)).isEqualTo(1);
        db.update("update fee_plans set course_fee=9999, monthly_due_day=1 where batch_id=?", batch);
        assertThat(db.queryForObject("select course_fee from enrollments where id=?", java.math.BigDecimal.class, id)).isEqualByComparingTo("1000");
        getAs("/enrollments/"+id+"/charges?size=2",admin).andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2)).andExpect(jsonPath("$.totalElements").value(5));
        getAs("/students/"+student+"/enrollments",admin).andExpect(jsonPath("$.totalElements").value(1));
        getAs("/batches/"+batch+"/enrollments",admin).andExpect(jsonPath("$.content[0].studentId").value(student));
    }

    @Test void duplicateRejectedButSameStudentCanEnrollInAnotherBatch() throws Exception {
        create(student,batch);
        postAs("/enrollments",request(student,batch),admin).andExpect(status().isConflict());
        long second = batch("2026/PB02");
        assertThat(create(student,second).path("registrationNumber").asText()).isEqualTo("2026/PB02/0001");
        assertThat(count("enrollments")).isEqualTo(2);
    }

    @Test void lecturerRosterRequiresCurrentAssignmentAndExposesNoFinancialOrNicData() throws Exception {
        create(student,batch);
        long lecturer=data.user("roster_lecturer","LECTURER",UserStatus.ACTIVE).getId();
        getAs("/batches/"+batch+"/students",lecturer).andExpect(status().isForbidden());
        var today=java.time.LocalDate.now();
        long assignment=body(postAs("/batches/"+batch+"/lecturers",Map.of(
                "lecturerUserId",lecturer,"assignmentStartDate",today.minusDays(1).toString(),
                "assignmentEndDate",today.plusDays(1).toString(),"status","ACTIVE"),admin)
                .andExpect(status().isCreated())).path("id").asLong();
        String content=getAs("/batches/"+batch+"/students",lecturer).andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].studentName").value("Test Student"))
                .andReturn().getResponse().getContentAsString();
        assertThat(content).doesNotContain("200012345678","originalAmount","finalPayableAmount","remarks");
        getAs("/enrollments",lecturer).andExpect(status().isForbidden());
        getAs("/enrollments/1/charges",lecturer).andExpect(status().isForbidden());
        db.update("update batch_lecturers set assignment_end_date=? where id=?",
                java.sql.Date.valueOf(today.minusDays(1)),assignment);
        getAs("/batches/"+batch+"/students",lecturer).andExpect(status().isForbidden());
    }

    @Test void enrollmentStatusChangesAreAuditedAndChargesRemain() throws Exception {
        long id=create(student,batch).path("id").asLong();
        mvc.perform(patch("/api/v1/enrollments/"+id+"/status").with(jwt().jwt(j->j.claim("userId",admin)))
                .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"SUSPENDED\",\"reason\":\"Paused\",\"version\":0}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("SUSPENDED"))
                .andExpect(jsonPath("$.version").value(1));
        mvc.perform(patch("/api/v1/enrollments/"+id+"/status").with(jwt().jwt(j->j.claim("userId",admin)))
                .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"CANCELLED\",\"reason\":\"Requested by student\",\"version\":1}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("CANCELLED"));
        mvc.perform(patch("/api/v1/enrollments/"+id+"/status").with(jwt().jwt(j->j.claim("userId",admin)))
                .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"ACTIVE\",\"reason\":\"Reopen\"}"))
                .andExpect(status().isBadRequest());
        assertThat(count("student_charges")).isEqualTo(5);
        assertThat(db.queryForObject("select count(*) from audit_logs where action='ENROLLMENT_STATUS_CHANGED'",Long.class)).isEqualTo(2);
    }

    @Test void batchIdentityCannotChangeAfterEnrollment() throws Exception {
        create(student,batch);
        var changed = Map.of("courseId", db.queryForObject("select course_id from course_batches where id=?", Long.class,batch),
                "branchId", 1, "batchNumber", "2026/PB99", "startDate", "2026-01-20",
                "endDate", "2026-03-31", "durationMonths", 3, "scheduleMode", "MANUAL", "status", "UPCOMING");
        mvc.perform(put("/api/v1/batches/"+batch).with(jwt().jwt(j->j.claim("userId",admin)))
                .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(changed)))
                .andExpect(status().isConflict());
        assertThat(db.queryForObject("select batch_number from course_batches where id=?",String.class,batch)).isEqualTo("2026/PB01");
    }

    @Test void concurrentEnrollmentsHaveUniqueSequentialNumbers() throws Exception {
        long other = student("200012345679");
        var executor = Executors.newFixedThreadPool(2);
        var gate = new CountDownLatch(1);
        try {
            Future<JsonNode> first = executor.submit(() -> { gate.await(); return create(student,batch); });
            Future<JsonNode> second = executor.submit(() -> { gate.await(); return create(other,batch); });
            gate.countDown();
            assertThat(java.util.Set.of(first.get(15,TimeUnit.SECONDS).path("registrationNumber").asText(),
                    second.get(15,TimeUnit.SECONDS).path("registrationNumber").asText()))
                    .containsExactlyInAnyOrder("2026/PB01/0001","2026/PB01/0002");
            assertThat(count("student_charges")).isEqualTo(10);
        } finally { executor.shutdownNow(); }
    }

    @Test void concurrentDuplicateCreatesOnlyOneEnrollment() throws Exception {
        var executor = Executors.newFixedThreadPool(2);
        var gate = new CountDownLatch(1);
        try {
            Callable<Integer> task = () -> { gate.await(); return postAs("/enrollments",request(student,batch),admin).andReturn().getResponse().getStatus(); };
            var first=executor.submit(task); var second=executor.submit(task); gate.countDown();
            assertThat(java.util.List.of(first.get(15,TimeUnit.SECONDS),second.get(15,TimeUnit.SECONDS))).containsExactlyInAnyOrder(201,409);
            assertThat(count("student_charges")).isEqualTo(5);
        } finally { executor.shutdownNow(); }
    }

    @Test void chargeFailureRollsBackEnrollmentAndSequence() {
        db.execute("alter table student_charges add constraint test_reject_charge check(original_amount < 1)");
        try {
            assertThatThrownBy(() -> create(student,batch)).hasRootCauseInstanceOf(java.sql.SQLException.class);
            assertThat(count("enrollments")).isZero();
            assertThat(count("student_charges")).isZero();
            assertThat(db.queryForObject("select registration_sequence from course_batches where id=?",Integer.class,batch)).isZero();
            assertThat(db.queryForObject("select count(*) from audit_logs where action='ENROLLMENT_CREATED'", Long.class)).isZero();
        } finally { db.execute("alter table student_charges drop constraint test_reject_charge"); }
    }

    @Test void rolesBranchesAndActiveBranchHeaderAreEnforced() throws Exception {
        long id=create(student,batch).path("id").asLong();
        data.branch("OTHER");
        long outsider=data.user("outsider","ADMIN",UserStatus.ACTIVE,"OTHER").getId();
        long lecturer=data.user("lecturer","LECTURER",UserStatus.ACTIVE).getId();
        for (long actor : new long[]{outsider, lecturer}) {
            getAs("/enrollments/"+id,actor).andExpect(status().isForbidden());
            getAs("/enrollments/"+id+"/charges",actor).andExpect(status().isForbidden());
            postAs("/enrollments/preview",request(student,batch),actor).andExpect(status().isForbidden());
            postAs("/enrollments",request(student,batch),actor).andExpect(status().isForbidden());
        }
        getAs("/enrollments",outsider).andExpect(jsonPath("$.totalElements").value(0));
        getAs("/enrollments?branchId=1",outsider).andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/enrollments").with(jwt().jwt(j -> j.claim("userId",outsider)))
                .header("X-Active-Branch-Id","1")).andExpect(status().isForbidden());
        long superAdmin=data.user("super","SUPER_ADMIN",UserStatus.ACTIVE,"OTHER").getId();
        getAs("/enrollments/"+id,superAdmin).andExpect(status().isOk());
        mvc.perform(get("/api/v1/enrollments")).andExpect(status().isUnauthorized());
    }

    @Test void inactiveRecordsMissingFeesAndStalePreviewAreRejected() throws Exception {
        db.update("update students set status='INACTIVE' where id=?",student);
        postAs("/enrollments",request(student,batch),admin).andExpect(status().isBadRequest());
        db.update("update students set status='ACTIVE' where id=?",student);
        db.update("update course_batches set status='COMPLETED' where id=?",batch);
        postAs("/enrollments",request(student,batch),admin).andExpect(status().isBadRequest());
        db.update("update course_batches set status='UPCOMING' where id=?",batch);
        var stale = new java.util.HashMap<String,Object>(request(student,batch)); stale.put("expectedFeePlanVersion",99);
        postAs("/enrollments",stale,admin).andExpect(status().isConflict());
        db.update("delete from fee_plans where batch_id=?",batch);
        postAs("/enrollments",request(student,batch),admin).andExpect(status().isBadRequest());
        assertThat(count("enrollments")).isZero();
    }

    @Test void cardIsSharedAcrossBatchesAndReplacementRevokesOldQr() throws Exception {
        create(student,batch);
        long second=batch("2026/PB02"); create(student,second);
        byte[] issued=postAs("/students/"+student+"/card",Map.of(),admin).andExpect(status().isOk())
                .andExpect(jsonPath("$.identifier").value("IHM-ST-"+String.format("%06d",student)))
                .andReturn().getResponse().getContentAsByteArray();
        assertThat(issued).isNotEmpty();
        postAs("/students/"+student+"/card",Map.of(),admin).andExpect(status().isOk());
        assertThat(count("student_cards")).isEqualTo(1);
        assertThat(count("student_card_events")).isEqualTo(1);
        byte[] qr=getAs("/students/"+student+"/card/qr",admin).andExpect(status().isOk())
                .andExpect(header().string("Cache-Control",org.hamcrest.Matchers.containsString("no-store")))
                .andReturn().getResponse().getContentAsByteArray();
        var image=javax.imageio.ImageIO.read(new java.io.ByteArrayInputStream(qr));
        String firstToken=new com.google.zxing.MultiFormatReader().decode(new com.google.zxing.BinaryBitmap(
                new com.google.zxing.common.HybridBinarizer(new com.google.zxing.client.j2se.BufferedImageLuminanceSource(image)))).getText();
        assertThat(firstToken).hasSize(43).doesNotContain("200012345678");
        assertThat(db.queryForObject("select token_hash from student_cards where student_id=?",String.class,student))
                .isNotEqualTo(firstToken);
        byte[] pdf=getAs("/students/"+student+"/card/pdf",admin).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsByteArray();
        try(var document=org.apache.pdfbox.Loader.loadPDF(pdf)) {
            assertThat(document.getNumberOfPages()).isEqualTo(2);
            assertThat(document.getPage(0).getMediaBox().getWidth()).isBetween(242f,244f);
            String text=new org.apache.pdfbox.text.PDFTextStripper().getText(document);
            assertThat(text).contains("Test Student","IHM-ST-").doesNotContain("200012345678");
            var printedBack=new org.apache.pdfbox.rendering.PDFRenderer(document).renderImageWithDPI(1,150);
            String printedToken=new com.google.zxing.MultiFormatReader().decode(new com.google.zxing.BinaryBitmap(
                    new com.google.zxing.common.HybridBinarizer(new com.google.zxing.client.j2se.BufferedImageLuminanceSource(printedBack)))).getText();
            assertThat(printedToken).isEqualTo(firstToken);
        }
        postAs("/students/"+student+"/card/replace",Map.of("reason","Lost card"),admin)
                .andExpect(status().isOk());
        byte[] newQr=getAs("/students/"+student+"/card/qr",admin).andReturn().getResponse().getContentAsByteArray();
        var newImage=javax.imageio.ImageIO.read(new java.io.ByteArrayInputStream(newQr));
        String newToken=new com.google.zxing.MultiFormatReader().decode(new com.google.zxing.BinaryBitmap(
                new com.google.zxing.common.HybridBinarizer(new com.google.zxing.client.j2se.BufferedImageLuminanceSource(newImage)))).getText();
        assertThat(newToken).isNotEqualTo(firstToken);
        assertThat(db.queryForObject("select count(*) from student_cards where token_hash=? and status='ACTIVE'",Long.class,
                java.util.HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(firstToken.getBytes(java.nio.charset.StandardCharsets.UTF_8))))).isZero();
        getAs("/students/"+student+"/card/history",admin).andExpect(jsonPath("$.totalElements").value(2));
        postAs("/students/"+student+"/card/revoke",Map.of("reason","Card returned"),admin)
                .andExpect(jsonPath("$.status").value("REVOKED"));
        getAs("/students/"+student+"/card/qr",admin).andExpect(status().isConflict());
        getAs("/students/"+student+"/card/pdf",admin).andExpect(status().isConflict());
        assertThat(db.queryForObject("select count(*) from audit_logs where action like 'STUDENT_CARD_%'",Long.class)).isEqualTo(3);
    }

    @Test void cardRequiresEnrollmentAndBranchAccess() throws Exception {
        postAs("/students/"+student+"/card",Map.of(),admin).andExpect(status().isBadRequest());
        getAs("/students/"+student+"/card",admin).andExpect(status().isNoContent());
        create(student,batch);
        data.branch("OTHER");
        long outsider=data.user("card_outsider","ADMIN",UserStatus.ACTIVE,"OTHER").getId();
        long lecturer=data.user("card_lecturer","LECTURER",UserStatus.ACTIVE).getId();
        for (long actor : new long[]{outsider,lecturer}) {
            getAs("/students/"+student+"/card",actor).andExpect(status().isForbidden());
            postAs("/students/"+student+"/card",Map.of(),actor).andExpect(status().isForbidden());
        }
        postAs("/students/"+student+"/card",Map.of(),admin).andExpect(status().isOk());
        postAs("/students/"+student+"/card/replace",Map.of(),admin).andExpect(status().isBadRequest());
        assertThat(count("student_card_events")).isEqualTo(1);
    }

    @Test void printableCardPreservesLatinSinhalaAndTamilNames() throws Exception {
        create(student,batch);
        postAs("/students/"+student+"/card",Map.of(),admin).andExpect(status().isOk());
        db.update("update students set full_name=? where id=?","José Perera",student);
        byte[] pdf=getAs("/students/"+student+"/card/pdf",admin).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsByteArray();
        try(var document=org.apache.pdfbox.Loader.loadPDF(pdf)) {
            assertThat(new org.apache.pdfbox.text.PDFTextStripper().getText(document)).contains("José Perera");
        }
        db.update("update students set full_name=? where id=?","නිමල් පෙරේරා",student);
        byte[] sinhala=getAs("/students/"+student+"/card/pdf",admin).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsByteArray();
        assertThat(sinhala).isNotEmpty();
        db.update("update students set full_name=? where id=?","ශ්‍රී නිමල් Perera",student);
        getAs("/students/"+student+"/card/pdf",admin).andExpect(status().isOk());
        db.update("update students set full_name=? where id=?","நிமல் பெரேரா",student);
        byte[] tamil=getAs("/students/"+student+"/card/pdf",admin).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsByteArray();
        assertThat(tamil).isNotEmpty();
        db.update("update students set full_name=? where id=?","Nimal நிமல்",student);
        getAs("/students/"+student+"/card/pdf",admin).andExpect(status().isOk());
        db.update("update students set full_name=? where id=?","Student 😀",student);
        getAs("/students/"+student+"/card/pdf",admin).andExpect(status().isBadRequest());
    }

    @Test void cardEmailNeedsSavedAddressAndConfiguredSender() throws Exception {
        create(student,batch);
        postAs("/students/"+student+"/card",Map.of(),admin).andExpect(status().isOk());
        postAs("/students/"+student+"/card/email",Map.of("idempotencyKey",java.util.UUID.randomUUID().toString()),admin)
                .andExpect(status().isBadRequest());
        db.update("update students set email='student@example.invalid' where id=?",student);
        postAs("/students/"+student+"/card/email",Map.of("idempotencyKey",java.util.UUID.randomUUID().toString()),admin)
                .andExpect(status().isServiceUnavailable());
        assertThat(count("document_deliveries")).isZero();
    }

    @Test void sequenceExhaustionDoesNotIssueFiveDigitRegistration() throws Exception {
        db.update("update course_batches set registration_sequence=9999 where id=?",batch);
        postAs("/enrollments",request(student,batch),admin).andExpect(status().isConflict());
        assertThat(count("enrollments")).isZero();
    }

    private long student(String nic) throws Exception {
        return body(postAs("/students",Map.of("fullName","Test Student","nic",nic,"contactNumber","0771234567","address","Colombo"),admin)
                .andExpect(status().isCreated())).path("id").asLong();
    }
    private long batch(String number) throws Exception {
        long course=body(postAs("/courses",Map.of("name",number,"shortCode","C"+number.replaceAll("[^0-9]",""),"status","ACTIVE"),admin)
                .andExpect(status().isCreated())).path("id").asLong();
        long id=body(postAs("/batches",Map.of("courseId",course,"branchId",1,"batchNumber",number,"startDate","2026-01-20",
                "endDate","2026-03-31","durationMonths",3,"scheduleMode","MANUAL","status","UPCOMING"),admin).andExpect(status().isCreated())).path("id").asLong();
        mvc.perform(put("/api/v1/batches/"+id+"/fee-plan").with(jwt().jwt(j->j.claim("userId",admin))).contentType(MediaType.APPLICATION_JSON)
                .content("{\"registrationFee\":100,\"courseFee\":1000,\"examinationFee\":500,\"durationMonths\":3,\"monthlyDueDay\":31,\"examinationDueDate\":\"2026-03-31\",\"currencyCode\":\"LKR\",\"status\":\"ACTIVE\"}"))
                .andExpect(status().isOk());
        return id;
    }
    private Map<String,Object> request(long student,long batch) { return Map.of("studentId",student,"batchId",batch,"enrollmentDate","2026-01-15"); }
    private JsonNode create(long student,long batch) throws Exception { return body(postAs("/enrollments",request(student,batch),admin).andExpect(status().isCreated())); }
    private ResultActions postAs(String path,Object body,long actor) throws Exception { return mvc.perform(post("/api/v1"+path).with(jwt().jwt(j->j.claim("userId",actor))).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(body))); }
    private ResultActions getAs(String path,long actor) throws Exception { return mvc.perform(get("/api/v1"+path).with(jwt().jwt(j->j.claim("userId",actor)))); }
    private JsonNode body(ResultActions result) throws Exception { return json.readTree(result.andReturn().getResponse().getContentAsString()); }
    private long count(String table) { return db.queryForObject("select count(*) from "+table,Long.class); }
}
