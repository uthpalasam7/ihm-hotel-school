package com.ihm.hotelschool.auth.dto;

import java.time.Instant;

public record AuthResponse(
		String accessToken,
		Instant accessTokenExpiresAt,
		String refreshToken,
		Instant refreshTokenExpiresAt,
		CurrentUserResponse user) {
}
