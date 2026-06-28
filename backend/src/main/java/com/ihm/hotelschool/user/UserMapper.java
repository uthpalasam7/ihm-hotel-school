package com.ihm.hotelschool.user;

import com.ihm.hotelschool.branch.Branch;
import com.ihm.hotelschool.user.dto.RoleResponse;
import com.ihm.hotelschool.user.dto.UserBranchResponse;
import com.ihm.hotelschool.user.dto.UserResponse;
import java.util.Comparator;
import org.springframework.stereotype.Component;

@Component
class UserMapper {

	UserResponse toResponse(UserAccount user) {
		return toResponse(user, null);
	}

	UserResponse toResponse(UserAccount user, String temporaryPassword) {
		return new UserResponse(
				user.getId(),
				user.getUsername(),
				user.getEmail(),
				user.getFullName(),
				user.getContactNumber(),
				user.getStatus(),
				user.getRoles().stream()
						.sorted(Comparator.comparing(Role::getCode))
						.map(role -> new RoleResponse(role.getId(), role.getCode(), role.getName()))
						.toList(),
				user.getBranches().stream()
						.sorted(Comparator.comparing(Branch::getCode))
						.map(branch -> new UserBranchResponse(branch.getId(), branch.getCode(), branch.getName()))
						.toList(),
				user.getLastLoginAt(),
				user.getCreatedAt(),
				user.getUpdatedAt(),
				user.getVersion(),
				temporaryPassword);
	}
}
