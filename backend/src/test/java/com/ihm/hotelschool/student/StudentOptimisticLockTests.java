package com.ihm.hotelschool.student;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ihm.hotelschool.auth.AuthTestData;
import com.ihm.hotelschool.user.UserAccount;
import com.ihm.hotelschool.user.UserStatus;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@SpringBootTest
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@Sql(statements = {
		"delete from refresh_tokens",
		"delete from audit_logs",
		"delete from batch_lecturers",
		"delete from fee_plans",
		"delete from course_batches",
		"delete from courses",
		"delete from students",
		"delete from user_roles",
		"delete from user_branches",
		"delete from users"
}, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class StudentOptimisticLockTests {

	@Autowired private StudentRepository studentRepository;
	@Autowired private AuthTestData authTestData;
	@Autowired private PlatformTransactionManager transactionManager;

	@Test
	void concurrentStudentUpdatesAreRejected() {
		UserAccount actor = authTestData.user("phase5_lock_admin", "ADMIN", UserStatus.ACTIVE);
		TransactionTemplate transaction = new TransactionTemplate(transactionManager);
		Long studentId = transaction.execute(status -> studentRepository.saveAndFlush(new Student(
				"Nimal Perera", "200012345678", "200012345678", "0712345678", null, null,
				"Kurunegala", null, null, null, Instant.now(), actor.getId())).getId());

		Student firstCopy = transaction.execute(status -> studentRepository.findById(studentId).orElseThrow());
		Student secondCopy = transaction.execute(status -> studentRepository.findById(studentId).orElseThrow());

		transaction.executeWithoutResult(status -> {
			firstCopy.updateDetails("Nimal Perera", firstCopy.getNic(), firstCopy.getNormalizedNic(), "0771111111",
					null, null, firstCopy.getAddress(), null, null, null, Instant.now(), actor.getId());
			studentRepository.saveAndFlush(firstCopy);
		});

		assertThatThrownBy(() -> transaction.executeWithoutResult(status -> {
			secondCopy.updateDetails("Nimal Perera", secondCopy.getNic(), secondCopy.getNormalizedNic(), "0772222222",
					null, null, secondCopy.getAddress(), null, null, null, Instant.now(), actor.getId());
			studentRepository.saveAndFlush(secondCopy);
		})).isInstanceOf(ObjectOptimisticLockingFailureException.class);
	}
}
