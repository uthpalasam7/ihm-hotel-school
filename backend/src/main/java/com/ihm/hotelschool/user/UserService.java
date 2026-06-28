package com.ihm.hotelschool.user;

import com.ihm.hotelschool.audit.AuditService;
import com.ihm.hotelschool.branch.Branch;
import com.ihm.hotelschool.branch.BranchRepository;
import com.ihm.hotelschool.branch.BranchStatus;
import com.ihm.hotelschool.common.security.CurrentActorService;
import com.ihm.hotelschool.common.security.CurrentActorService.CurrentActor;
import com.ihm.hotelschool.common.web.ConflictException;
import com.ihm.hotelschool.common.web.NotFoundException;
import com.ihm.hotelschool.common.web.PageResponse;
import com.ihm.hotelschool.auth.RefreshTokenRepository;
import com.ihm.hotelschool.user.dto.ResetPasswordRequest;
import com.ihm.hotelschool.user.dto.UserBranchesRequest;
import com.ihm.hotelschool.user.dto.UserProfileRequest;
import com.ihm.hotelschool.user.dto.UserRequest;
import com.ihm.hotelschool.user.dto.UserResponse;
import com.ihm.hotelschool.user.dto.UserRolesRequest;
import com.ihm.hotelschool.user.dto.UserStatusRequest;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.servlet.http.HttpServletRequest;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class UserService {

	private static final String TEMP_PASSWORD_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789";

	private final UserRepository userRepository;
	private final RoleRepository roleRepository;
	private final BranchRepository branchRepository;
	private final RefreshTokenRepository refreshTokenRepository;
	private final PasswordEncoder passwordEncoder;
	private final UserMapper userMapper;
	private final CurrentActorService currentActorService;
	private final AuditService auditService;
	private final Clock clock;
	private final SecureRandom secureRandom = new SecureRandom();

	UserService(
			UserRepository userRepository,
			RoleRepository roleRepository,
			BranchRepository branchRepository,
			RefreshTokenRepository refreshTokenRepository,
			PasswordEncoder passwordEncoder,
			UserMapper userMapper,
			CurrentActorService currentActorService,
			AuditService auditService,
			Clock clock) {
		this.userRepository = userRepository;
		this.roleRepository = roleRepository;
		this.branchRepository = branchRepository;
		this.refreshTokenRepository = refreshTokenRepository;
		this.passwordEncoder = passwordEncoder;
		this.userMapper = userMapper;
		this.currentActorService = currentActorService;
		this.auditService = auditService;
		this.clock = clock;
	}

	@Transactional(readOnly = true)
	PageResponse<UserResponse> list(String role, Long branchId, String status, String search, Pageable pageable, Authentication authentication) {
		CurrentActor actor = adminActor(authentication);
		Pageable safePageable = PageRequest.of(pageable.getPageNumber(), Math.min(pageable.getPageSize(), 100), Sort.by("fullName").ascending());
		Specification<UserAccount> spec = Specification.where(null);
		if (!actor.superAdmin()) {
			Set<Long> actorBranchIds = actor.branchIds();
			spec = spec.and((root, query, criteriaBuilder) -> {
				query.distinct(true);
				Join<UserAccount, Branch> branchJoin = root.join("branches", JoinType.INNER);
				Join<UserAccount, Role> roleJoin = root.join("roles", JoinType.INNER);
				return criteriaBuilder.and(
						branchJoin.get("id").in(actorBranchIds),
						criteriaBuilder.equal(roleJoin.get("code"), "LECTURER"));
			});
		}
		if (role != null && !role.isBlank()) {
			String roleCode = role.trim().toUpperCase(Locale.ROOT);
			spec = spec.and((root, query, criteriaBuilder) -> {
				query.distinct(true);
				return criteriaBuilder.equal(root.join("roles", JoinType.INNER).get("code"), roleCode);
			});
		}
		if (branchId != null) {
			if (!actor.superAdmin() && !actor.branchIds().contains(branchId)) {
				throw new AccessDeniedException("Access denied");
			}
			spec = spec.and((root, query, criteriaBuilder) -> {
				query.distinct(true);
				return criteriaBuilder.equal(root.join("branches", JoinType.INNER).get("id"), branchId);
			});
		}
		if (status != null && !status.isBlank()) {
			UserStatus userStatus = UserStatus.valueOf(status.trim().toUpperCase(Locale.ROOT));
			spec = spec.and((root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get("status"), userStatus));
		}
		String normalizedSearch = blankToNull(search);
		if (normalizedSearch != null) {
			String like = "%" + normalizedSearch.toLowerCase(Locale.ROOT) + "%";
			spec = spec.and((root, query, criteriaBuilder) -> criteriaBuilder.or(
					criteriaBuilder.like(criteriaBuilder.lower(root.get("username")), like),
					criteriaBuilder.like(criteriaBuilder.lower(root.get("fullName")), like),
					criteriaBuilder.like(criteriaBuilder.lower(root.get("email")), like)));
		}
		return PageResponse.from(userRepository.findAll(spec, safePageable), userMapper::toResponse);
	}

	@Transactional(readOnly = true)
	UserResponse get(Long id, Authentication authentication) {
		CurrentActor actor = adminActor(authentication);
		UserAccount user = findUser(id);
		requireTargetManageable(actor, user);
		return userMapper.toResponse(user);
	}

	@Transactional
	UserResponse create(UserRequest request, Authentication authentication, HttpServletRequest httpRequest) {
		CurrentActor actor = adminActor(authentication);
		String username = normalizeRequired(request.username());
		String email = blankToNull(request.email());
		assertUniqueUsername(username, null);
		assertUniqueEmail(email, null);
		Set<Role> roles = resolveRoles(normalizeRoleCodes(request.roleCodes()));
		Set<Branch> branches = resolveBranches(request.branchIds());
		requireAssignable(actor, roles, branches);

		Instant now = clock.instant();
		String temporaryPassword = blankToNull(request.temporaryPassword());
		if (temporaryPassword == null) {
			temporaryPassword = generateTemporaryPassword();
		}
		UserStatus status = request.status() == UserStatus.DISABLED ? UserStatus.DISABLED : UserStatus.PASSWORD_CHANGE_REQUIRED;
		UserAccount user = new UserAccount(
				username,
				email,
				passwordEncoder.encode(temporaryPassword),
				normalizeRequired(request.fullName()),
				status,
				now);
		user.setCreatedBy(actor.id());
		user.replaceRoles(roles, now, actor.id());
		user.replaceBranches(branches, now, actor.id());
		UserAccount saved = userRepository.save(user);
		UserResponse response = userMapper.toResponse(saved, temporaryPassword);
		auditService.record(actor.user(), primaryBranch(branches), "USER_CREATED", "User", saved.getId(), null, userMapper.toResponse(saved), null, httpRequest);
		return response;
	}

	@Transactional
	UserResponse updateProfile(Long id, UserProfileRequest request, Authentication authentication, HttpServletRequest httpRequest) {
		CurrentActor actor = adminActor(authentication);
		UserAccount user = findUser(id);
		requireTargetManageable(actor, user);
		UserResponse oldValue = userMapper.toResponse(user);
		String username = normalizeRequired(request.username());
		String email = blankToNull(request.email());
		assertUniqueUsername(username, id);
		assertUniqueEmail(email, id);
		user.updateProfile(username, email, normalizeRequired(request.fullName()), blankToNull(request.contactNumber()), clock.instant(), actor.id());
		UserResponse response = userMapper.toResponse(user);
		auditService.record(actor.user(), primaryBranch(user.getBranches()), "USER_UPDATED", "User", user.getId(), oldValue, response, null, httpRequest);
		return response;
	}

	@Transactional
	UserResponse changeStatus(Long id, UserStatusRequest request, Authentication authentication, HttpServletRequest httpRequest) {
		CurrentActor actor = adminActor(authentication);
		UserAccount user = findUser(id);
		requireTargetManageable(actor, user);
		if (actor.id().equals(id) && request.status() == UserStatus.DISABLED) {
			throw new ConflictException("You cannot disable your own account");
		}
		UserResponse oldValue = userMapper.toResponse(user);
		user.changeStatus(request.status(), clock.instant(), actor.id());
		if (request.status() == UserStatus.DISABLED) {
			refreshTokenRepository.revokeActiveTokensForUser(user, clock.instant());
		}
		UserResponse response = userMapper.toResponse(user);
		auditService.record(actor.user(), primaryBranch(user.getBranches()), "USER_STATUS_CHANGED", "User", user.getId(), oldValue, response, request.reason(), httpRequest);
		return response;
	}

	@Transactional
	UserResponse replaceRoles(Long id, UserRolesRequest request, Authentication authentication, HttpServletRequest httpRequest) {
		CurrentActor actor = adminActor(authentication);
		UserAccount user = findUser(id);
		requireTargetManageable(actor, user);
		Set<Role> roles = resolveRoles(normalizeRoleCodes(request.roleCodes()));
		requireAssignable(actor, roles, user.getBranches());
		UserResponse oldValue = userMapper.toResponse(user);
		user.replaceRoles(roles, clock.instant(), actor.id());
		UserResponse response = userMapper.toResponse(user);
		auditService.record(actor.user(), primaryBranch(user.getBranches()), "USER_ROLES_CHANGED", "User", user.getId(), oldValue, response, request.reason(), httpRequest);
		return response;
	}

	@Transactional
	UserResponse replaceBranches(Long id, UserBranchesRequest request, Authentication authentication, HttpServletRequest httpRequest) {
		CurrentActor actor = adminActor(authentication);
		UserAccount user = findUser(id);
		requireTargetManageable(actor, user);
		Set<Branch> branches = resolveBranches(request.branchIds());
		requireAssignable(actor, user.getRoles(), branches);
		UserResponse oldValue = userMapper.toResponse(user);
		user.replaceBranches(branches, clock.instant(), actor.id());
		UserResponse response = userMapper.toResponse(user);
		auditService.record(actor.user(), primaryBranch(branches), "USER_BRANCHES_CHANGED", "User", user.getId(), oldValue, response, request.reason(), httpRequest);
		return response;
	}

	@Transactional
	UserResponse resetPassword(Long id, ResetPasswordRequest request, Authentication authentication, HttpServletRequest httpRequest) {
		CurrentActor actor = adminActor(authentication);
		UserAccount user = findUser(id);
		requireTargetManageable(actor, user);
		String temporaryPassword = blankToNull(request.temporaryPassword());
		if (temporaryPassword == null) {
			temporaryPassword = generateTemporaryPassword();
		}
		UserResponse oldValue = userMapper.toResponse(user);
		Instant now = clock.instant();
		user.resetPassword(passwordEncoder.encode(temporaryPassword), now, actor.id());
		refreshTokenRepository.revokeActiveTokensForUser(user, now);
		UserResponse response = userMapper.toResponse(user, temporaryPassword);
		auditService.record(actor.user(), primaryBranch(user.getBranches()), "USER_PASSWORD_RESET", "User", user.getId(), oldValue, userMapper.toResponse(user), request.reason(), httpRequest);
		return response;
	}

	private CurrentActor adminActor(Authentication authentication) {
		CurrentActor actor = currentActorService.actor(authentication);
		actor.requireAnyRole("SUPER_ADMIN", "ADMIN");
		return actor;
	}

	private UserAccount findUser(Long id) {
		return userRepository.findWithRolesAndBranchesById(id).orElseThrow(() -> new NotFoundException("User was not found"));
	}

	private Set<Role> resolveRoles(Set<String> roleCodes) {
		Set<Role> roles = roleRepository.findByCodeIn(roleCodes);
		Set<String> resolved = roles.stream().map(Role::getCode).collect(Collectors.toSet());
		if (!resolved.equals(roleCodes)) {
			throw new IllegalArgumentException("One or more roles are invalid");
		}
		return roles;
	}

	private Set<Branch> resolveBranches(Set<Long> branchIds) {
		List<Branch> branches = branchRepository.findAllById(branchIds).stream()
				.filter(branch -> branch.getStatus() == BranchStatus.ACTIVE)
				.toList();
		if (branches.size() != branchIds.size()) {
			throw new IllegalArgumentException("One or more branches are invalid");
		}
		return new HashSet<>(branches);
	}

	private void requireTargetManageable(CurrentActor actor, UserAccount target) {
		if (actor.superAdmin()) {
			return;
		}
		boolean lecturerOnly = target.getRoles().stream().allMatch(role -> role.getCode().equals("LECTURER"));
		if (!lecturerOnly) {
			throw new AccessDeniedException("Access denied");
		}
		actor.requireBranchAccess(target.getBranches().stream().map(Branch::getId).collect(Collectors.toSet()));
	}

	private void requireAssignable(CurrentActor actor, Set<Role> roles, Set<Branch> branches) {
		if (actor.superAdmin()) {
			return;
		}
		boolean lecturerOnly = roles.stream().allMatch(role -> role.getCode().equals("LECTURER"));
		if (!lecturerOnly) {
			throw new AccessDeniedException("Access denied");
		}
		actor.requireBranchAccess(branches.stream().map(Branch::getId).collect(Collectors.toSet()));
	}

	private void assertUniqueUsername(String username, Long existingId) {
		boolean exists = existingId == null
				? userRepository.existsByUsername(username)
				: userRepository.existsByUsernameAndIdNot(username, existingId);
		if (exists) {
			throw new ConflictException("Username already exists");
		}
	}

	private void assertUniqueEmail(String email, Long existingId) {
		if (email == null) {
			return;
		}
		boolean exists = existingId == null
				? userRepository.existsByEmail(email)
				: userRepository.existsByEmailAndIdNot(email, existingId);
		if (exists) {
			throw new ConflictException("Email already exists");
		}
	}

	private Set<String> normalizeRoleCodes(Set<String> roleCodes) {
		return roleCodes.stream()
				.map(code -> code.trim().toUpperCase(Locale.ROOT))
				.collect(Collectors.toSet());
	}

	private String normalizeRequired(String value) {
		return value.trim();
	}

	private String blankToNull(String value) {
		String normalized = value == null ? null : value.trim();
		return normalized == null || normalized.isBlank() ? null : normalized;
	}

	private Branch primaryBranch(Set<Branch> branches) {
		return branches.stream().findFirst().orElse(null);
	}

	private String generateTemporaryPassword() {
		StringBuilder password = new StringBuilder(14);
		for (int i = 0; i < 14; i++) {
			password.append(TEMP_PASSWORD_CHARS.charAt(secureRandom.nextInt(TEMP_PASSWORD_CHARS.length())));
		}
		return password.toString();
	}
}
