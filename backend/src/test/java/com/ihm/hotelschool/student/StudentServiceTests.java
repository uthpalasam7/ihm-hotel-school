package com.ihm.hotelschool.student;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.ihm.hotelschool.audit.AuditService;
import com.ihm.hotelschool.branch.BranchRepository;
import com.ihm.hotelschool.common.security.CurrentActorService;
import com.ihm.hotelschool.common.security.CurrentActorService.CurrentActor;
import com.ihm.hotelschool.common.web.ConflictException;
import com.ihm.hotelschool.student.dto.StudentRequest;
import com.ihm.hotelschool.student.photo.StudentPhotoManager;
import com.ihm.hotelschool.user.Role;
import com.ihm.hotelschool.user.UserAccount;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.core.Authentication;

class StudentServiceTests {

	@Test
	void databaseNicRaceIsReturnedAsConflict() {
		StudentRepository repository = mock(StudentRepository.class);
		CurrentActorService actorService = mock(CurrentActorService.class);
		UserAccount user = mock(UserAccount.class);
		Role role = mock(Role.class);
		when(role.getCode()).thenReturn("ADMIN");
		when(user.getId()).thenReturn(7L);
		when(user.getRoles()).thenReturn(java.util.Set.of(role));
		CurrentActor actor = new CurrentActor(user);
		Authentication authentication = mock(Authentication.class);
		when(actorService.actor(authentication)).thenReturn(actor);
		when(repository.existsByNormalizedNic("200012345678")).thenReturn(false);
		when(repository.saveAndFlush(any(Student.class)))
				.thenThrow(new DataIntegrityViolationException("concurrent unique constraint violation"));

		StudentService service = new StudentService(repository, mock(BranchRepository.class), mock(StudentMapper.class),
				new NicNormalizer(), mock(StudentPhotoManager.class), actorService, mock(AuditService.class),
				Clock.fixed(Instant.parse("2026-07-21T00:00:00Z"), ZoneOffset.UTC));
		StudentRequest request = new StudentRequest("Nimal Perera", "2000 12345678", "0712345678", null,
				null, "Kurunegala", null, null, null);

		assertThatThrownBy(() -> service.create(request, authentication, Optional.empty(), null))
				.isInstanceOf(ConflictException.class)
				.hasMessage("A student with this NIC already exists");
	}
}
