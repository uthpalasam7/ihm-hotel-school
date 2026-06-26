package com.ihm.hotelschool.auth;

import com.ihm.hotelschool.common.config.ApplicationProperties;
import com.ihm.hotelschool.user.UserAccount;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RefreshTokenService {

	private final RefreshTokenRepository refreshTokenRepository;
	private final ApplicationProperties properties;
	private final SecureRandom secureRandom = new SecureRandom();

	public RefreshTokenService(RefreshTokenRepository refreshTokenRepository, ApplicationProperties properties) {
		this.refreshTokenRepository = refreshTokenRepository;
		this.properties = properties;
	}

	@Transactional
	public IssuedRefreshToken issue(UserAccount user, Instant now, String deviceInfo) {
		byte[] tokenBytes = new byte[48];
		secureRandom.nextBytes(tokenBytes);
		String rawToken = Base64.getUrlEncoder().withoutPadding().encodeToString(tokenBytes);
		Instant expiresAt = now.plus(properties.security().refreshTokenDays(), ChronoUnit.DAYS);
		refreshTokenRepository.save(new RefreshToken(user, hash(rawToken), expiresAt, now, deviceInfo));
		return new IssuedRefreshToken(rawToken, expiresAt);
	}

	public String hash(String rawToken) {
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			byte[] hashed = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
			return Base64.getEncoder().encodeToString(hashed);
		} catch (NoSuchAlgorithmException exception) {
			throw new IllegalStateException("SHA-256 is not available", exception);
		}
	}

	public record IssuedRefreshToken(String token, Instant expiresAt) {
	}
}
