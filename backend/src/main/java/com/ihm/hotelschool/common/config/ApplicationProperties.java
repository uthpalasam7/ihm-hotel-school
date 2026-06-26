package com.ihm.hotelschool.common.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app")
public record ApplicationProperties(
		String timezone,
		String currencyCode,
		@Valid Security security,
		@Valid InitialAdmin initialAdmin) {

	public record Security(
			@NotBlank String jwtSecret,
			@Min(1) long accessTokenMinutes,
			@Min(1) long refreshTokenDays,
			@Min(1) int maxFailedLoginAttempts,
			@Min(1) long failedLoginLockMinutes) {
	}

	public record InitialAdmin(String username, String password, String fullName, String email) {
		public boolean configured() {
			return username != null && !username.isBlank() && password != null && !password.isBlank();
		}
	}
}
