package com.ihm.hotelschool.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UserProfileRequest(
		@NotBlank @Size(max = 100) String username,
		@Size(max = 200) String email,
		@NotBlank @Size(max = 200) String fullName,
		@Size(max = 30) String contactNumber) {
}
