package com.ihm.hotelschool.batch;

import com.ihm.hotelschool.batch.dto.BatchLecturerRequest;
import com.ihm.hotelschool.batch.dto.BatchLecturerResponse;
import com.ihm.hotelschool.batch.dto.BatchLecturerStatusRequest;
import com.ihm.hotelschool.batch.dto.BatchLecturerSyncRequest;
import com.ihm.hotelschool.batch.dto.BatchRequest;
import com.ihm.hotelschool.batch.dto.BatchResponse;
import com.ihm.hotelschool.batch.dto.BatchStatusRequest;
import com.ihm.hotelschool.batch.dto.FeePlanRequest;
import com.ihm.hotelschool.batch.dto.FeePlanResponse;
import com.ihm.hotelschool.batch.dto.InstallmentPreviewResponse;
import com.ihm.hotelschool.common.security.ActiveBranchContextService;
import com.ihm.hotelschool.common.web.PageResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
class BatchController {

	private final BatchService batchService;
	private final ActiveBranchContextService activeBranchContextService;

	BatchController(BatchService batchService, ActiveBranchContextService activeBranchContextService) {
		this.batchService = batchService;
		this.activeBranchContextService = activeBranchContextService;
	}

	@GetMapping("/batches")
	PageResponse<BatchResponse> list(
			@RequestParam(required = false) Long branchId,
			@RequestParam(required = false) Long courseId,
			@RequestParam(required = false) Long lecturerId,
			@RequestParam(required = false) String status,
			@RequestParam(required = false) LocalDate startDateFrom,
			@RequestParam(required = false) LocalDate startDateTo,
			@RequestParam(required = false) String search,
			Pageable pageable,
			Authentication authentication,
			HttpServletRequest httpRequest) {
		Long effectiveBranchId = branchId != null
				? branchId
				: activeBranchContextService.activeBranchId(httpRequest, authentication).orElse(null);
		return batchService.list(effectiveBranchId, courseId, lecturerId, status, startDateFrom, startDateTo, search, pageable, authentication);
	}

	@PostMapping("/batches")
	@ResponseStatus(HttpStatus.CREATED)
	BatchResponse create(
			@Valid @RequestBody BatchRequest request,
			Authentication authentication,
			HttpServletRequest httpRequest) {
		return batchService.create(request, authentication, httpRequest);
	}

	@GetMapping("/batches/{id}")
	BatchResponse get(@PathVariable Long id, Authentication authentication) {
		return batchService.get(id, authentication);
	}

	@PutMapping("/batches/{id}")
	BatchResponse update(
			@PathVariable Long id,
			@Valid @RequestBody BatchRequest request,
			Authentication authentication,
			HttpServletRequest httpRequest) {
		return batchService.update(id, request, authentication, httpRequest);
	}

	@PatchMapping("/batches/{id}/status")
	BatchResponse changeStatus(
			@PathVariable Long id,
			@Valid @RequestBody BatchStatusRequest request,
			Authentication authentication,
			HttpServletRequest httpRequest) {
		return batchService.changeStatus(id, request, authentication, httpRequest);
	}

	@GetMapping("/batches/{batchId}/fee-plan")
	FeePlanResponse getFeePlan(@PathVariable Long batchId, Authentication authentication) {
		return batchService.getFeePlan(batchId, authentication);
	}

	@PostMapping("/batches/{batchId}/fee-plan")
	FeePlanResponse createFeePlan(
			@PathVariable Long batchId,
			@Valid @RequestBody FeePlanRequest request,
			Authentication authentication,
			HttpServletRequest httpRequest) {
		return batchService.saveFeePlan(batchId, request, authentication, httpRequest);
	}

	@PutMapping("/batches/{batchId}/fee-plan")
	FeePlanResponse updateFeePlan(
			@PathVariable Long batchId,
			@Valid @RequestBody FeePlanRequest request,
			Authentication authentication,
			HttpServletRequest httpRequest) {
		return batchService.saveFeePlan(batchId, request, authentication, httpRequest);
	}

	@PostMapping("/batches/{batchId}/fee-plan/installment-preview")
	InstallmentPreviewResponse installmentPreview(
			@PathVariable Long batchId,
			@Valid @RequestBody FeePlanRequest request,
			Authentication authentication) {
		return batchService.previewFeePlan(batchId, request, authentication);
	}

	@GetMapping("/batches/{batchId}/lecturers")
	List<BatchLecturerResponse> listLecturers(@PathVariable Long batchId, Authentication authentication) {
		return batchService.listLecturers(batchId, authentication);
	}

	@PostMapping("/batches/{batchId}/lecturers")
	@ResponseStatus(HttpStatus.CREATED)
	BatchLecturerResponse addLecturer(
			@PathVariable Long batchId,
			@Valid @RequestBody BatchLecturerRequest request,
			Authentication authentication,
			HttpServletRequest httpRequest) {
		return batchService.addLecturer(batchId, request, authentication, httpRequest);
	}

	@PutMapping("/batches/{batchId}/lecturers")
	List<BatchLecturerResponse> syncLecturers(
			@PathVariable Long batchId,
			@Valid @RequestBody BatchLecturerSyncRequest request,
			Authentication authentication,
			HttpServletRequest httpRequest) {
		return batchService.syncLecturers(batchId, request, authentication, httpRequest);
	}

	@PutMapping("/batches/{batchId}/lecturers/{assignmentId}")
	BatchLecturerResponse updateLecturer(
			@PathVariable Long batchId,
			@PathVariable Long assignmentId,
			@Valid @RequestBody BatchLecturerRequest request,
			Authentication authentication,
			HttpServletRequest httpRequest) {
		return batchService.updateLecturer(batchId, assignmentId, request, authentication, httpRequest);
	}

	@PatchMapping("/batches/{batchId}/lecturers/{assignmentId}/status")
	BatchLecturerResponse changeLecturerStatus(
			@PathVariable Long batchId,
			@PathVariable Long assignmentId,
			@Valid @RequestBody BatchLecturerStatusRequest request,
			Authentication authentication,
			HttpServletRequest httpRequest) {
		return batchService.changeLecturerStatus(batchId, assignmentId, request, authentication, httpRequest);
	}
}
