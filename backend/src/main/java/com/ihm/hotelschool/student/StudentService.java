package com.ihm.hotelschool.student;

import com.ihm.hotelschool.audit.AuditService;
import com.ihm.hotelschool.branch.Branch;
import com.ihm.hotelschool.branch.BranchRepository;
import com.ihm.hotelschool.common.security.CurrentActorService;
import com.ihm.hotelschool.common.security.CurrentActorService.CurrentActor;
import com.ihm.hotelschool.common.web.ConflictException;
import com.ihm.hotelschool.common.web.NotFoundException;
import com.ihm.hotelschool.common.web.PageResponse;
import com.ihm.hotelschool.student.dto.StudentRequest;
import com.ihm.hotelschool.student.dto.StudentResponse;
import com.ihm.hotelschool.student.dto.StudentStatusRequest;
import com.ihm.hotelschool.student.photo.PhotoStorageException;
import com.ihm.hotelschool.student.photo.PhotoVariant;
import com.ihm.hotelschool.student.photo.StoredPhotoContent;
import com.ihm.hotelschool.student.photo.StudentPhotoManager;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Clock;
import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

@Service
class StudentService {

	private static final Logger LOGGER = LoggerFactory.getLogger(StudentService.class);

	private final StudentRepository studentRepository;
	private final BranchRepository branchRepository;
	private final StudentMapper studentMapper;
	private final NicNormalizer nicNormalizer;
	private final StudentPhotoManager photoManager;
	private final CurrentActorService currentActorService;
	private final AuditService auditService;
	private final Clock clock;

	StudentService(StudentRepository studentRepository, BranchRepository branchRepository, StudentMapper studentMapper,
			NicNormalizer nicNormalizer, StudentPhotoManager photoManager, CurrentActorService currentActorService,
			AuditService auditService, Clock clock) {
		this.studentRepository = studentRepository;
		this.branchRepository = branchRepository;
		this.studentMapper = studentMapper;
		this.nicNormalizer = nicNormalizer;
		this.photoManager = photoManager;
		this.currentActorService = currentActorService;
		this.auditService = auditService;
		this.clock = clock;
	}

	@Transactional(readOnly = true)
	PageResponse<StudentResponse> list(String status, String nic, String search, Pageable pageable, Authentication authentication) {
		adminActor(authentication);
		Pageable safePageable = PageRequest.of(pageable.getPageNumber(), Math.min(pageable.getPageSize(), 100),
				Sort.by("fullName").ascending().and(Sort.by("id").ascending()));
		Specification<Student> specification = Specification.where(null);
		if (status != null && !status.isBlank()) {
			StudentStatus studentStatus = parseStatus(status);
			specification = specification.and((root, query, criteriaBuilder) ->
					criteriaBuilder.equal(root.get("status"), studentStatus));
		}
		if (nic != null && !nic.isBlank()) {
			String normalizedNic = nicNormalizer.normalizedValue(nic);
			specification = specification.and((root, query, criteriaBuilder) ->
					criteriaBuilder.equal(root.get("normalizedNic"), normalizedNic));
		}
		String normalizedSearch = blankToNull(search);
		if (normalizedSearch != null) {
			String textLike = "%" + normalizedSearch.toLowerCase(Locale.ROOT) + "%";
			String nicLike = "%" + nicNormalizer.normalizedValue(normalizedSearch) + "%";
			specification = specification.and((root, query, criteriaBuilder) -> criteriaBuilder.or(
					criteriaBuilder.like(criteriaBuilder.lower(root.get("fullName")), textLike),
					criteriaBuilder.like(root.get("normalizedNic"), nicLike),
					criteriaBuilder.like(root.get("contactNumber"), "%" + normalizedSearch + "%"),
					criteriaBuilder.like(root.get("alternativeContactNumber"), "%" + normalizedSearch + "%")));
		}
		return PageResponse.from(studentRepository.findAll(specification, safePageable), studentMapper::toResponse);
	}

	@Transactional(readOnly = true)
	StudentResponse get(Long id, Authentication authentication) {
		adminActor(authentication);
		return studentMapper.toResponse(findStudent(id));
	}

	@Transactional(readOnly = true)
	StudentResponse findByNic(String nic, Authentication authentication) {
		adminActor(authentication);
		Student student = studentRepository.findByNormalizedNic(nicNormalizer.normalizedValue(nic))
				.orElseThrow(() -> new NotFoundException("Student was not found"));
		return studentMapper.toResponse(student);
	}

	@Transactional
	StudentResponse create(StudentRequest request, Authentication authentication, Optional<Long> auditBranchId,
			HttpServletRequest httpRequest) {
		CurrentActor actor = adminActor(authentication);
		String normalizedNic = nicNormalizer.normalizedValue(request.nic());
		if (studentRepository.existsByNormalizedNic(normalizedNic)) {
			throw new ConflictException("A student with this NIC already exists");
		}
		Instant now = clock.instant();
		Student student = new Student(
				normalizeRequired(request.fullName()),
				nicNormalizer.displayValue(request.nic()),
				normalizedNic,
				normalizeRequired(request.contactNumber()),
				blankToNull(request.alternativeContactNumber()),
				blankToNull(request.email()),
				normalizeRequired(request.address()),
				request.dateOfBirth(),
				blankToNull(request.gender()),
				blankToNull(request.remarks()),
				now,
				actor.id());
		try {
			student = studentRepository.saveAndFlush(student);
		} catch (DataIntegrityViolationException exception) {
			throw new ConflictException("A student with this NIC already exists");
		}
		StudentResponse response = studentMapper.toResponse(student);
		auditService.record(actor.user(), auditBranch(auditBranchId), "STUDENT_CREATED", "Student", student.getId(),
				null, response, null, httpRequest);
		return response;
	}

	@Transactional
	StudentResponse update(Long id, StudentRequest request, Authentication authentication, Optional<Long> auditBranchId,
			HttpServletRequest httpRequest) {
		CurrentActor actor = adminActor(authentication);
		Student student = findStudent(id);
		StudentResponse oldValue = studentMapper.toResponse(student);
		String normalizedNic = nicNormalizer.normalizedValue(request.nic());
		if (studentRepository.existsByNormalizedNicAndIdNot(normalizedNic, id)) {
			throw new ConflictException("A student with this NIC already exists");
		}
		student.updateDetails(
				normalizeRequired(request.fullName()),
				nicNormalizer.displayValue(request.nic()),
				normalizedNic,
				normalizeRequired(request.contactNumber()),
				blankToNull(request.alternativeContactNumber()),
				blankToNull(request.email()),
				normalizeRequired(request.address()),
				request.dateOfBirth(),
				blankToNull(request.gender()),
				blankToNull(request.remarks()),
				clock.instant(),
				actor.id());
		try {
			studentRepository.saveAndFlush(student);
		} catch (DataIntegrityViolationException exception) {
			throw new ConflictException("A student with this NIC already exists");
		}
		StudentResponse response = studentMapper.toResponse(student);
		auditService.record(actor.user(), auditBranch(auditBranchId), "STUDENT_UPDATED", "Student", student.getId(),
				oldValue, response, null, httpRequest);
		return response;
	}

	@Transactional
	StudentResponse changeStatus(Long id, StudentStatusRequest request, Authentication authentication,
			Optional<Long> auditBranchId, HttpServletRequest httpRequest) {
		CurrentActor actor = adminActor(authentication);
		Student student = findStudent(id);
		StudentResponse oldValue = studentMapper.toResponse(student);
		student.changeStatus(request.status(), clock.instant(), actor.id());
		studentRepository.saveAndFlush(student);
		StudentResponse response = studentMapper.toResponse(student);
		auditService.record(actor.user(), auditBranch(auditBranchId), "STUDENT_STATUS_CHANGED", "Student", student.getId(),
				oldValue, response, request.reason(), httpRequest);
		return response;
	}

	@Transactional
	StudentResponse uploadPhoto(Long id, MultipartFile file, Authentication authentication, Optional<Long> auditBranchId,
			HttpServletRequest httpRequest) {
		CurrentActor actor = adminActor(authentication);
		Student student = findStudent(id);
		String oldKey = student.getPhotoStorageKey();
		String newKey = photoManager.store(file);
		boolean synchronizedCleanup = false;
		try {
			student.changePhoto(newKey, clock.instant(), actor.id());
			studentRepository.saveAndFlush(student);
			registerPhotoReplacement(newKey, oldKey);
			synchronizedCleanup = true;
			StudentResponse response = studentMapper.toResponse(student);
			auditService.record(actor.user(), auditBranch(auditBranchId), "STUDENT_PHOTO_UPDATED", "Student", student.getId(),
					Map.of("photoAvailable", oldKey != null), Map.of("photoAvailable", true), null, httpRequest);
			return response;
		} catch (RuntimeException exception) {
			if (!synchronizedCleanup) {
				deletePhotoQuietly(newKey);
			}
			throw exception;
		}
	}

	@Transactional(readOnly = true)
	StoredPhotoContent loadPhoto(Long id, PhotoVariant variant, Authentication authentication) {
		adminActor(authentication);
		Student student = findStudent(id);
		if (student.getPhotoStorageKey() == null) {
			throw new NotFoundException("Student photo was not found");
		}
		return photoManager.load(student.getPhotoStorageKey(), variant);
	}

	@Transactional
	void deletePhoto(Long id, Authentication authentication, Optional<Long> auditBranchId, HttpServletRequest httpRequest) {
		CurrentActor actor = adminActor(authentication);
		Student student = findStudent(id);
		String oldKey = student.getPhotoStorageKey();
		if (oldKey == null) {
			return;
		}
		student.changePhoto(null, clock.instant(), actor.id());
		studentRepository.saveAndFlush(student);
		registerPhotoDeletion(oldKey);
		auditService.record(actor.user(), auditBranch(auditBranchId), "STUDENT_PHOTO_REMOVED", "Student", student.getId(),
				Map.of("photoAvailable", true), Map.of("photoAvailable", false), null, httpRequest);
	}

	private CurrentActor adminActor(Authentication authentication) {
		CurrentActor actor = currentActorService.actor(authentication);
		actor.requireAnyRole("SUPER_ADMIN", "ADMIN");
		return actor;
	}

	private Student findStudent(Long id) {
		return studentRepository.findById(id).orElseThrow(() -> new NotFoundException("Student was not found"));
	}

	private Branch auditBranch(Optional<Long> branchId) {
		return branchId.flatMap(branchRepository::findById).orElse(null);
	}

	private StudentStatus parseStatus(String status) {
		try {
			return StudentStatus.valueOf(status.trim().toUpperCase(Locale.ROOT));
		} catch (IllegalArgumentException exception) {
			throw new IllegalArgumentException("Student status is invalid");
		}
	}

	private void registerPhotoReplacement(String newKey, String oldKey) {
		TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
			@Override
			public void afterCompletion(int status) {
				if (status == TransactionSynchronization.STATUS_COMMITTED) {
					deletePhotoQuietly(oldKey);
				} else {
					deletePhotoQuietly(newKey);
				}
			}
		});
	}

	private void registerPhotoDeletion(String oldKey) {
		TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
			@Override
			public void afterCommit() {
				deletePhotoQuietly(oldKey);
			}
		});
	}

	private void deletePhotoQuietly(String storageKey) {
		if (storageKey == null) {
			return;
		}
		try {
			photoManager.delete(storageKey);
		} catch (PhotoStorageException exception) {
			LOGGER.warn("An obsolete student photo could not be removed from storage");
		}
	}

	private String normalizeRequired(String value) {
		return value.trim();
	}

	private String blankToNull(String value) {
		String normalized = value == null ? null : value.trim();
		return normalized == null || normalized.isBlank() ? null : normalized;
	}
}
