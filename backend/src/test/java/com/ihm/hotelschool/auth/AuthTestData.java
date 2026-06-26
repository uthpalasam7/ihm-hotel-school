package com.ihm.hotelschool.auth;

import com.ihm.hotelschool.branch.Branch;
import com.ihm.hotelschool.branch.BranchRepository;
import com.ihm.hotelschool.user.Role;
import com.ihm.hotelschool.user.RoleRepository;
import com.ihm.hotelschool.user.UserAccount;
import com.ihm.hotelschool.user.UserRepository;
import com.ihm.hotelschool.user.UserStatus;
import java.time.Instant;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
class AuthTestData {

	private final UserRepository userRepository;
	private final RoleRepository roleRepository;
	private final BranchRepository branchRepository;
	private final PasswordEncoder passwordEncoder;

	AuthTestData(
			UserRepository userRepository,
			RoleRepository roleRepository,
			BranchRepository branchRepository,
			PasswordEncoder passwordEncoder) {
		this.userRepository = userRepository;
		this.roleRepository = roleRepository;
		this.branchRepository = branchRepository;
		this.passwordEncoder = passwordEncoder;
	}

	@Transactional
	UserAccount user(String username, String roleCode, UserStatus status) {
		Instant now = Instant.now();
		Role role = roleRepository.findByCode(roleCode).orElseThrow();
		Branch branch = branchRepository.findByCode("IHM-MAIN").orElseThrow();
		UserAccount user = new UserAccount(
				username,
				username + "@example.invalid",
				passwordEncoder.encode("Password123"),
				username + " User",
				status,
				now);
		user.addRole(role);
		user.addBranch(branch);
		return userRepository.saveAndFlush(user);
	}
}
