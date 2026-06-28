package com.ihm.hotelschool.user;

import com.ihm.hotelschool.common.security.CurrentActorService;
import com.ihm.hotelschool.common.security.CurrentActorService.CurrentActor;
import com.ihm.hotelschool.user.dto.RoleResponse;
import java.util.Comparator;
import java.util.List;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class RoleService {

	private final RoleRepository roleRepository;
	private final CurrentActorService currentActorService;

	RoleService(RoleRepository roleRepository, CurrentActorService currentActorService) {
		this.roleRepository = roleRepository;
		this.currentActorService = currentActorService;
	}

	@Transactional(readOnly = true)
	List<RoleResponse> assignableRoles(Authentication authentication) {
		CurrentActor actor = currentActorService.actor(authentication);
		actor.requireAnyRole("SUPER_ADMIN", "ADMIN");
		return roleRepository.findAll().stream()
				.filter(role -> actor.superAdmin() || role.getCode().equals("LECTURER"))
				.sorted(Comparator.comparing(Role::getCode))
				.map(role -> new RoleResponse(role.getId(), role.getCode(), role.getName()))
				.toList();
	}
}
