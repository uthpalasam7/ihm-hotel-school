package com.ihm.hotelschool.branch.dto;

import com.ihm.hotelschool.branch.BranchStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record BranchRequest(
		@NotBlank @Size(max = 30) String code,
		@NotBlank @Size(max = 150) String name,
		String address,
		@Size(max = 30) String contactNumber,
		@NotNull BranchStatus status) {
}
