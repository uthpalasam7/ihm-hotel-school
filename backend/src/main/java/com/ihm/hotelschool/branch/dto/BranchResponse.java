package com.ihm.hotelschool.branch.dto;

import com.ihm.hotelschool.branch.BranchStatus;
import java.time.Instant;

public record BranchResponse(
		Long id,
		String code,
		String name,
		String address,
		String contactNumber,
		BranchStatus status,
		boolean defaultBranch,
		Instant createdAt,
		Instant updatedAt,
		Long version) {
}
