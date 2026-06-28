package com.ihm.hotelschool.common.security;

import com.ihm.hotelschool.common.web.NotFoundException;
import com.ihm.hotelschool.user.UserAccount;
import com.ihm.hotelschool.user.UserRepository;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

@Service
public class CurrentActorService {

	private final UserRepository userRepository;

	public CurrentActorService(UserRepository userRepository) {
		this.userRepository = userRepository;
	}

	public CurrentActor actor(Authentication authentication) {
		Jwt jwt = jwt(authentication);
		Long userId = jwt.getClaim("userId");
		if (userId == null) {
			throw new AccessDeniedException("Access denied");
		}
		UserAccount user = userRepository.findWithRolesAndBranchesById(userId)
				.orElseThrow(() -> new NotFoundException("Authenticated user was not found"));
		return new CurrentActor(user);
	}

	private Jwt jwt(Authentication authentication) {
		if (authentication == null || !(authentication.getPrincipal() instanceof Jwt jwt)) {
			throw new AccessDeniedException("Access denied");
		}
		return jwt;
	}

	public record CurrentActor(UserAccount user) {
		public Long id() {
			return user.getId();
		}

		public boolean hasRole(String roleCode) {
			return user.getRoles().stream().anyMatch(role -> role.getCode().equals(roleCode));
		}

		public boolean superAdmin() {
			return hasRole("SUPER_ADMIN");
		}

		public boolean admin() {
			return hasRole("ADMIN");
		}

		public Set<Long> branchIds() {
			Set<Long> branchIds = new HashSet<>();
			user.getBranches().forEach(branch -> branchIds.add(branch.getId()));
			return branchIds;
		}

		public void requireAnyRole(String... roleCodes) {
			List<String> allowed = List.of(roleCodes);
			if (user.getRoles().stream().noneMatch(role -> allowed.contains(role.getCode()))) {
				throw new AccessDeniedException("Access denied");
			}
		}

		public void requireBranchAccess(Collection<Long> requestedBranchIds) {
			if (superAdmin()) {
				return;
			}
			if (!branchIds().containsAll(requestedBranchIds)) {
				throw new AccessDeniedException("Access denied");
			}
		}
	}
}
