package com.ihm.hotelschool.auth;

import com.ihm.hotelschool.auth.dto.CurrentUserResponse;
import com.ihm.hotelschool.branch.Branch;
import com.ihm.hotelschool.user.Role;
import com.ihm.hotelschool.user.UserAccount;
import java.util.Comparator;
import org.springframework.stereotype.Component;

@Component
class AuthMapper {

	CurrentUserResponse toCurrentUser(UserAccount user) {
		return new CurrentUserResponse(
				user.getId(),
				user.getUsername(),
				user.getFullName(),
				user.getStatus().name(),
				user.requiresPasswordChange(),
				user.getRoles().stream().map(Role::getCode).sorted().toList(),
				user.getBranches().stream()
						.sorted(Comparator.comparing(Branch::getCode))
						.map(branch -> new CurrentUserResponse.BranchResponse(branch.getId(), branch.getCode(), branch.getName()))
						.toList());
	}
}
