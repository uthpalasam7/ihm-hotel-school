package com.ihm.hotelschool.user.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.ihm.hotelschool.user.UserStatus;
import java.time.Instant;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record UserResponse(
		Long id,
		String username,
		String email,
		String fullName,
		String contactNumber,
		UserStatus status,
		List<RoleResponse> roles,
		List<UserBranchResponse> branches,
		Instant lastLoginAt,
		Instant createdAt,
		Instant updatedAt,
		Long version,
		String temporaryPassword) {
}
