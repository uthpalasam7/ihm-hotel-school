package com.ihm.hotelschool.student.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record StudentRequest(
		@NotBlank @Size(max = 250) String fullName,
		@NotBlank @Size(max = 30) @Pattern(regexp = "\\s*[A-Za-z0-9]+\\s*", message = "NIC must contain numbers and letters only") String nic,
		@NotBlank @Pattern(regexp = "\\s*\\d{10}\\s*", message = "Contact number must be exactly 10 digits") String contactNumber,
		@Size(max = 30) @Pattern(regexp = "\\s*|\\s*\\d{10}\\s*", message = "Alternative contact must be exactly 10 digits") String alternativeContactNumber,
		@Email @Size(max = 200) String email,
		@NotBlank String address,
		LocalDate dateOfBirth,
		@Size(max = 30) @Pattern(regexp = "\\s*|Male|Female|Other", message = "Gender must be Male, Female, or Other") String gender,
		String remarks) {
}
