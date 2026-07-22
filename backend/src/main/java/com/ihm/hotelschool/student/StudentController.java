package com.ihm.hotelschool.student;

import com.ihm.hotelschool.common.security.ActiveBranchContextService;
import com.ihm.hotelschool.common.web.PageResponse;
import com.ihm.hotelschool.student.dto.StudentRequest;
import com.ihm.hotelschool.student.dto.StudentResponse;
import com.ihm.hotelschool.student.dto.StudentStatusRequest;
import com.ihm.hotelschool.student.photo.PhotoVariant;
import com.ihm.hotelschool.student.photo.StoredPhotoContent;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.Optional;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Pageable;
import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/students")
class StudentController {

	private final StudentService studentService;
	private final ActiveBranchContextService activeBranchContextService;

	StudentController(StudentService studentService, ActiveBranchContextService activeBranchContextService) {
		this.studentService = studentService;
		this.activeBranchContextService = activeBranchContextService;
	}

	@GetMapping
	PageResponse<StudentResponse> list(@RequestParam(required = false) String status,
			@RequestParam(required = false) String nic,
			@RequestParam(required = false) String search,
			Pageable pageable, Authentication authentication, HttpServletRequest request) {
		validateActiveBranch(request, authentication);
		return studentService.list(status, nic, search, pageable, authentication);
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	StudentResponse create(@Valid @RequestBody StudentRequest studentRequest, Authentication authentication,
			HttpServletRequest request) {
		return studentService.create(studentRequest, authentication, activeBranch(request, authentication), request);
	}

	@GetMapping("/{id}")
	StudentResponse get(@PathVariable Long id, Authentication authentication, HttpServletRequest request) {
		validateActiveBranch(request, authentication);
		return studentService.get(id, authentication);
	}

	@PutMapping("/{id}")
	StudentResponse update(@PathVariable Long id, @Valid @RequestBody StudentRequest studentRequest,
			Authentication authentication, HttpServletRequest request) {
		return studentService.update(id, studentRequest, authentication, activeBranch(request, authentication), request);
	}

	@PatchMapping("/{id}/status")
	StudentResponse changeStatus(@PathVariable Long id, @Valid @RequestBody StudentStatusRequest statusRequest,
			Authentication authentication, HttpServletRequest request) {
		return studentService.changeStatus(id, statusRequest, authentication, activeBranch(request, authentication), request);
	}

	@GetMapping("/by-nic/{nic}")
	StudentResponse findByNic(@PathVariable String nic, Authentication authentication, HttpServletRequest request) {
		validateActiveBranch(request, authentication);
		return studentService.findByNic(nic, authentication);
	}

	@PostMapping(path = "/{id}/photo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	StudentResponse uploadPhoto(@PathVariable Long id, @RequestPart("photo") MultipartFile photo,
			Authentication authentication, HttpServletRequest request) {
		return studentService.uploadPhoto(id, photo, authentication, activeBranch(request, authentication), request);
	}

	@GetMapping("/{id}/photo")
	ResponseEntity<Resource> getPhoto(@PathVariable Long id,
			@RequestParam(required = false, defaultValue = "full") String variant,
			Authentication authentication, HttpServletRequest request) {
		validateActiveBranch(request, authentication);
		StoredPhotoContent photo = studentService.loadPhoto(id, PhotoVariant.from(variant), authentication);
		ByteArrayResource resource = new ByteArrayResource(photo.content());
		return ResponseEntity.ok()
				.contentType(MediaType.parseMediaType(photo.mediaType()))
				.contentLength(photo.content().length)
				.cacheControl(CacheControl.noStore().cachePrivate())
				.header(HttpHeaders.CONTENT_DISPOSITION,
						ContentDisposition.inline().filename("student-photo." + photo.extension()).build().toString())
				.header("X-Content-Type-Options", "nosniff")
				.body(resource);
	}

	@DeleteMapping("/{id}/photo")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	void deletePhoto(@PathVariable Long id, Authentication authentication, HttpServletRequest request) {
		studentService.deletePhoto(id, authentication, activeBranch(request, authentication), request);
	}

	private Optional<Long> activeBranch(HttpServletRequest request, Authentication authentication) {
		return activeBranchContextService.activeBranchId(request, authentication);
	}

	private void validateActiveBranch(HttpServletRequest request, Authentication authentication) {
		activeBranch(request, authentication);
	}
}
