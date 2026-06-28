package com.ihm.hotelschool.user;

import com.ihm.hotelschool.common.security.ActiveBranchContextService;
import com.ihm.hotelschool.common.web.PageResponse;
import com.ihm.hotelschool.user.dto.ResetPasswordRequest;
import com.ihm.hotelschool.user.dto.RoleResponse;
import com.ihm.hotelschool.user.dto.UserBranchesRequest;
import com.ihm.hotelschool.user.dto.UserProfileRequest;
import com.ihm.hotelschool.user.dto.UserRequest;
import com.ihm.hotelschool.user.dto.UserResponse;
import com.ihm.hotelschool.user.dto.UserRolesRequest;
import com.ihm.hotelschool.user.dto.UserStatusRequest;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users")
class UserController {

	private final UserService userService;
	private final RoleService roleService;
	private final ActiveBranchContextService activeBranchContextService;

	UserController(UserService userService, RoleService roleService, ActiveBranchContextService activeBranchContextService) {
		this.userService = userService;
		this.roleService = roleService;
		this.activeBranchContextService = activeBranchContextService;
	}

	@GetMapping
	PageResponse<UserResponse> list(
			@RequestParam(required = false) String role,
			@RequestParam(required = false) Long branchId,
			@RequestParam(required = false) String status,
			@RequestParam(required = false) String search,
			Pageable pageable,
			Authentication authentication,
			HttpServletRequest httpRequest) {
		Long effectiveBranchId = branchId != null
				? branchId
				: activeBranchContextService.activeBranchId(httpRequest, authentication).orElse(null);
		return userService.list(role, effectiveBranchId, status, search, pageable, authentication);
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	UserResponse create(
			@Valid @RequestBody UserRequest request,
			Authentication authentication,
			HttpServletRequest httpRequest) {
		return userService.create(request, authentication, httpRequest);
	}

	@GetMapping("/{id}")
	UserResponse get(@PathVariable Long id, Authentication authentication) {
		return userService.get(id, authentication);
	}

	@PutMapping("/{id}")
	UserResponse updateProfile(
			@PathVariable Long id,
			@Valid @RequestBody UserProfileRequest request,
			Authentication authentication,
			HttpServletRequest httpRequest) {
		return userService.updateProfile(id, request, authentication, httpRequest);
	}

	@PatchMapping("/{id}/status")
	UserResponse changeStatus(
			@PathVariable Long id,
			@Valid @RequestBody UserStatusRequest request,
			Authentication authentication,
			HttpServletRequest httpRequest) {
		return userService.changeStatus(id, request, authentication, httpRequest);
	}

	@PutMapping("/{id}/roles")
	UserResponse replaceRoles(
			@PathVariable Long id,
			@Valid @RequestBody UserRolesRequest request,
			Authentication authentication,
			HttpServletRequest httpRequest) {
		return userService.replaceRoles(id, request, authentication, httpRequest);
	}

	@PutMapping("/{id}/branches")
	UserResponse replaceBranches(
			@PathVariable Long id,
			@Valid @RequestBody UserBranchesRequest request,
			Authentication authentication,
			HttpServletRequest httpRequest) {
		return userService.replaceBranches(id, request, authentication, httpRequest);
	}

	@PostMapping("/{id}/reset-password")
	UserResponse resetPassword(
			@PathVariable Long id,
			@Valid @RequestBody ResetPasswordRequest request,
			Authentication authentication,
			HttpServletRequest httpRequest) {
		return userService.resetPassword(id, request, authentication, httpRequest);
	}

	@GetMapping("/roles")
	List<RoleResponse> assignableRoles(Authentication authentication) {
		return roleService.assignableRoles(authentication);
	}
}
