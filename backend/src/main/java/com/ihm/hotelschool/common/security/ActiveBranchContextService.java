package com.ihm.hotelschool.common.security;

import com.ihm.hotelschool.branch.BranchRepository;
import com.ihm.hotelschool.branch.BranchStatus;
import com.ihm.hotelschool.common.security.CurrentActorService.CurrentActor;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Optional;
import java.util.Set;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

@Service
public class ActiveBranchContextService {

	public static final String ACTIVE_BRANCH_HEADER = "X-Active-Branch-Id";

	private final CurrentActorService currentActorService;
	private final BranchRepository branchRepository;

	public ActiveBranchContextService(CurrentActorService currentActorService, BranchRepository branchRepository) {
		this.currentActorService = currentActorService;
		this.branchRepository = branchRepository;
	}

	public Optional<Long> activeBranchId(HttpServletRequest request, Authentication authentication) {
		String headerValue = request.getHeader(ACTIVE_BRANCH_HEADER);
		if (headerValue == null || headerValue.isBlank()) {
			return Optional.empty();
		}
		Long branchId = parseBranchId(headerValue);
		if (!branchRepository.existsByIdAndStatus(branchId, BranchStatus.ACTIVE)) {
			throw new AccessDeniedException("Access denied");
		}
		CurrentActor actor = currentActorService.actor(authentication);
		actor.requireBranchAccess(Set.of(branchId));
		return Optional.of(branchId);
	}

	private Long parseBranchId(String headerValue) {
		try {
			return Long.valueOf(headerValue.trim());
		} catch (NumberFormatException exception) {
			throw new IllegalArgumentException("Active branch id is invalid");
		}
	}
}
