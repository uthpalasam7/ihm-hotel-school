package com.ihm.hotelschool.user.dto;

import com.ihm.hotelschool.user.UserStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.Set;

public record UserRequest(
		@NotBlank @Size(max = 100) String username,
		@Size(max = 200) String email,
		@NotBlank @Size(max = 200) String fullName,
		@Size(max = 30) String contactNumber,
		@NotNull UserStatus status,
		@NotEmpty Set<String> roleCodes,
		@NotEmpty Set<Long> branchIds,
		@Size(min = 8, max = 128) String temporaryPassword) {
}
