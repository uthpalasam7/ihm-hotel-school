package com.ihm.hotelschool.auth;

import com.ihm.hotelschool.branch.BranchRepository;
import com.ihm.hotelschool.common.config.ApplicationProperties;
import com.ihm.hotelschool.user.RoleRepository;
import com.ihm.hotelschool.user.UserAccount;
import com.ihm.hotelschool.user.UserRepository;
import com.ihm.hotelschool.user.UserStatus;
import java.time.Clock;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
class InitialAdminBootstrap implements CommandLineRunner {

	private static final Logger LOGGER = LoggerFactory.getLogger(InitialAdminBootstrap.class);

	private final ApplicationProperties properties;
	private final UserRepository userRepository;
	private final RoleRepository roleRepository;
	private final BranchRepository branchRepository;
	private final PasswordEncoder passwordEncoder;
	private final Clock clock;

	InitialAdminBootstrap(
			ApplicationProperties properties,
			UserRepository userRepository,
			RoleRepository roleRepository,
			BranchRepository branchRepository,
			PasswordEncoder passwordEncoder,
			Clock clock) {
		this.properties = properties;
		this.userRepository = userRepository;
		this.roleRepository = roleRepository;
		this.branchRepository = branchRepository;
		this.passwordEncoder = passwordEncoder;
		this.clock = clock;
	}

	@Override
	@Transactional
	public void run(String... args) {
		if (userRepository.count() > 0) {
			return;
		}
		if (!properties.initialAdmin().configured()) {
			LOGGER.warn("No users exist. Set IHM_INITIAL_ADMIN_USERNAME and IHM_INITIAL_ADMIN_PASSWORD to bootstrap the first super administrator.");
			return;
		}

		Instant now = clock.instant();
		UserAccount user = new UserAccount(
				properties.initialAdmin().username(),
				blankToNull(properties.initialAdmin().email()),
				passwordEncoder.encode(properties.initialAdmin().password()),
				properties.initialAdmin().fullName(),
				UserStatus.PASSWORD_CHANGE_REQUIRED,
				now);
		user.addRole(roleRepository.findByCode("SUPER_ADMIN").orElseThrow());
		user.addBranch(branchRepository.findByCode("IHM-MAIN").orElseThrow());
		userRepository.save(user);
		LOGGER.info("Created initial super administrator '{}'. Password change is required.", user.getUsername());
	}

	private String blankToNull(String value) {
		return value == null || value.isBlank() ? null : value;
	}
}
