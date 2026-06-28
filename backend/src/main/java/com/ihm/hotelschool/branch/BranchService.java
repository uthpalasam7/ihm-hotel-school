package com.ihm.hotelschool.branch;

import com.ihm.hotelschool.audit.AuditService;
import com.ihm.hotelschool.branch.dto.BranchRequest;
import com.ihm.hotelschool.branch.dto.BranchResponse;
import com.ihm.hotelschool.branch.dto.BranchStatusRequest;
import com.ihm.hotelschool.common.security.CurrentActorService;
import com.ihm.hotelschool.common.security.CurrentActorService.CurrentActor;
import com.ihm.hotelschool.common.web.ConflictException;
import com.ihm.hotelschool.common.web.NotFoundException;
import com.ihm.hotelschool.common.web.PageResponse;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Clock;
import java.time.Instant;
import java.util.Set;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class BranchService {

	private final BranchRepository branchRepository;
	private final BranchMapper branchMapper;
	private final CurrentActorService currentActorService;
	private final AuditService auditService;
	private final Clock clock;

	BranchService(
			BranchRepository branchRepository,
			BranchMapper branchMapper,
			CurrentActorService currentActorService,
			AuditService auditService,
			Clock clock) {
		this.branchRepository = branchRepository;
		this.branchMapper = branchMapper;
		this.currentActorService = currentActorService;
		this.auditService = auditService;
		this.clock = clock;
	}

	@Transactional(readOnly = true)
	PageResponse<BranchResponse> list(String status, String search, Pageable pageable, Authentication authentication) {
		CurrentActor actor = currentActorService.actor(authentication);
		actor.requireAnyRole("SUPER_ADMIN", "ADMIN", "LECTURER");
		Pageable safePageable = PageRequest.of(pageable.getPageNumber(), Math.min(pageable.getPageSize(), 100), Sort.by("name").ascending());
		Specification<Branch> spec = Specification.where(null);
		if (!actor.superAdmin()) {
			Set<Long> branchIds = actor.branchIds();
			spec = spec.and((root, query, criteriaBuilder) -> root.get("id").in(branchIds));
		}
		if (status != null && !status.isBlank()) {
			BranchStatus branchStatus = BranchStatus.valueOf(status.trim().toUpperCase());
			spec = spec.and((root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get("status"), branchStatus));
		}
		String normalizedSearch = blankToNull(search);
		if (normalizedSearch != null) {
			String like = "%" + normalizedSearch.toLowerCase() + "%";
			spec = spec.and((root, query, criteriaBuilder) -> criteriaBuilder.or(
					criteriaBuilder.like(criteriaBuilder.lower(root.get("name")), like),
					criteriaBuilder.like(criteriaBuilder.lower(root.get("code")), like)));
		}
		return PageResponse.from(branchRepository.findAll(spec, safePageable), branchMapper::toResponse);
	}

	@Transactional(readOnly = true)
	BranchResponse get(Long id, Authentication authentication) {
		CurrentActor actor = currentActorService.actor(authentication);
		actor.requireAnyRole("SUPER_ADMIN", "ADMIN", "LECTURER");
		if (!actor.superAdmin() && !actor.branchIds().contains(id)) {
			throw new AccessDeniedException("Access denied");
		}
		return branchMapper.toResponse(findBranch(id));
	}

	@Transactional
	BranchResponse create(BranchRequest request, Authentication authentication, HttpServletRequest httpRequest) {
		CurrentActor actor = superAdmin(authentication);
		String code = normalizeCode(request.code());
		if (branchRepository.existsByCode(code)) {
			throw new ConflictException("Branch code already exists");
		}
		Instant now = clock.instant();
		Branch branch = branchRepository.save(new Branch(
				code,
				normalizeText(request.name()),
				blankToNull(request.address()),
				blankToNull(request.contactNumber()),
				request.status(),
				false,
				now,
				actor.id()));
		BranchResponse response = branchMapper.toResponse(branch);
		auditService.record(actor.user(), branch, "BRANCH_CREATED", "Branch", branch.getId(), null, response, null, httpRequest);
		return response;
	}

	@Transactional
	BranchResponse update(Long id, BranchRequest request, Authentication authentication, HttpServletRequest httpRequest) {
		CurrentActor actor = superAdmin(authentication);
		Branch branch = findBranch(id);
		BranchResponse oldValue = branchMapper.toResponse(branch);
		String code = normalizeCode(request.code());
		if (branchRepository.existsByCodeAndIdNot(code, id)) {
			throw new ConflictException("Branch code already exists");
		}
		assertCanDeactivateDefault(branch, request.status());
		branch.updateDetails(
				code,
				normalizeText(request.name()),
				blankToNull(request.address()),
				blankToNull(request.contactNumber()),
				request.status(),
				clock.instant(),
				actor.id());
		BranchResponse response = branchMapper.toResponse(branch);
		auditService.record(actor.user(), branch, "BRANCH_UPDATED", "Branch", branch.getId(), oldValue, response, null, httpRequest);
		return response;
	}

	@Transactional
	BranchResponse changeStatus(Long id, BranchStatusRequest request, Authentication authentication, HttpServletRequest httpRequest) {
		CurrentActor actor = superAdmin(authentication);
		Branch branch = findBranch(id);
		BranchResponse oldValue = branchMapper.toResponse(branch);
		assertCanDeactivateDefault(branch, request.status());
		branch.changeStatus(request.status(), clock.instant(), actor.id());
		BranchResponse response = branchMapper.toResponse(branch);
		auditService.record(actor.user(), branch, "BRANCH_STATUS_CHANGED", "Branch", branch.getId(), oldValue, response, request.reason(), httpRequest);
		return response;
	}

	private CurrentActor superAdmin(Authentication authentication) {
		CurrentActor actor = currentActorService.actor(authentication);
		if (!actor.superAdmin()) {
			throw new AccessDeniedException("Access denied");
		}
		return actor;
	}

	private Branch findBranch(Long id) {
		return branchRepository.findById(id).orElseThrow(() -> new NotFoundException("Branch was not found"));
	}

	private void assertCanDeactivateDefault(Branch branch, BranchStatus status) {
		if (branch.isDefaultBranch() && status == BranchStatus.INACTIVE && branchRepository.countByStatus(BranchStatus.ACTIVE) <= 1) {
			throw new ConflictException("The only active default branch cannot be deactivated");
		}
	}

	private String normalizeCode(String value) {
		return normalizeText(value).toUpperCase();
	}

	private String normalizeText(String value) {
		return value == null ? null : value.trim();
	}

	private String blankToNull(String value) {
		String normalized = normalizeText(value);
		return normalized == null || normalized.isBlank() ? null : normalized;
	}
}
