package com.ihm.hotelschool.auth;

import com.ihm.hotelschool.common.security.BranchAuthorizationService;
import java.util.Map;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth-check")
class AuthCheckController {

	private final BranchAuthorizationService branchAuthorizationService;

	AuthCheckController(BranchAuthorizationService branchAuthorizationService) {
		this.branchAuthorizationService = branchAuthorizationService;
	}

	@GetMapping("/admin")
	@PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
	Map<String, String> admin() {
		return Map.of("status", "allowed");
	}

	@GetMapping("/lecturer")
	@PreAuthorize("hasRole('LECTURER')")
	Map<String, String> lecturer() {
		return Map.of("status", "allowed");
	}

	@GetMapping("/branches/{branchId}")
	Map<String, String> branch(@PathVariable Long branchId, Authentication authentication) {
		branchAuthorizationService.requireBranchAccess(authentication, branchId);
		return Map.of("status", "allowed");
	}
}
