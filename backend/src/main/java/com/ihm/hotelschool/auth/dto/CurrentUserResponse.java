package com.ihm.hotelschool.auth.dto;

import java.util.List;

public record CurrentUserResponse(
		Long id,
		String username,
		String fullName,
		String status,
		boolean passwordChangeRequired,
		List<String> roles,
		List<BranchResponse> branches) {

	public record BranchResponse(Long id, String code, String name) {
	}
}
