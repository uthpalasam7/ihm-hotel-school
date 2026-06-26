package com.ihm.hotelschool.common.security;

import java.util.Collection;
import java.util.List;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

@Service
public class BranchAuthorizationService {

	public void requireBranchAccess(Authentication authentication, Long branchId) {
		if (authentication == null || !(authentication.getPrincipal() instanceof Jwt jwt)) {
			throw new AccessDeniedException("Access denied");
		}
		List<String> roles = jwt.getClaimAsStringList("roles");
		if (roles != null && roles.contains("SUPER_ADMIN")) {
			return;
		}
		Object branchIds = jwt.getClaims().get("branchIds");
		if (!(branchIds instanceof Collection<?> values) || values.stream().map(String::valueOf).noneMatch(branchId.toString()::equals)) {
			throw new AccessDeniedException("Access denied");
		}
	}
}
