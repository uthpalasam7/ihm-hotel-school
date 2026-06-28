package com.ihm.hotelschool.branch;

import com.ihm.hotelschool.branch.dto.BranchRequest;
import com.ihm.hotelschool.branch.dto.BranchResponse;
import com.ihm.hotelschool.branch.dto.BranchStatusRequest;
import com.ihm.hotelschool.common.web.PageResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
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
@RequestMapping("/api/v1/branches")
class BranchController {

	private final BranchService branchService;

	BranchController(BranchService branchService) {
		this.branchService = branchService;
	}

	@GetMapping
	PageResponse<BranchResponse> list(
			@RequestParam(required = false) String status,
			@RequestParam(required = false) String search,
			Pageable pageable,
			Authentication authentication) {
		return branchService.list(status, search, pageable, authentication);
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	BranchResponse create(
			@Valid @RequestBody BranchRequest request,
			Authentication authentication,
			HttpServletRequest httpRequest) {
		return branchService.create(request, authentication, httpRequest);
	}

	@GetMapping("/{id}")
	BranchResponse get(@PathVariable Long id, Authentication authentication) {
		return branchService.get(id, authentication);
	}

	@PutMapping("/{id}")
	BranchResponse update(
			@PathVariable Long id,
			@Valid @RequestBody BranchRequest request,
			Authentication authentication,
			HttpServletRequest httpRequest) {
		return branchService.update(id, request, authentication, httpRequest);
	}

	@PatchMapping("/{id}/status")
	BranchResponse changeStatus(
			@PathVariable Long id,
			@Valid @RequestBody BranchStatusRequest request,
			Authentication authentication,
			HttpServletRequest httpRequest) {
		return branchService.changeStatus(id, request, authentication, httpRequest);
	}
}
