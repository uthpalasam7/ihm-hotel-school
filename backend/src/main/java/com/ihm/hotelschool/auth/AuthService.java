package com.ihm.hotelschool.auth;

import com.ihm.hotelschool.auth.JwtTokenService.AccessToken;
import com.ihm.hotelschool.auth.RefreshTokenService.IssuedRefreshToken;
import com.ihm.hotelschool.auth.dto.AuthResponse;
import com.ihm.hotelschool.auth.dto.ChangePasswordRequest;
import com.ihm.hotelschool.auth.dto.CurrentUserResponse;
import com.ihm.hotelschool.auth.dto.LoginRequest;
import com.ihm.hotelschool.auth.dto.RefreshTokenRequest;
import com.ihm.hotelschool.user.UserAccount;
import com.ihm.hotelschool.user.UserRepository;
import java.time.Clock;
import java.time.Instant;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

	private final AuthenticationManager authenticationManager;
	private final UserRepository userRepository;
	private final RefreshTokenRepository refreshTokenRepository;
	private final JwtTokenService jwtTokenService;
	private final RefreshTokenService refreshTokenService;
	private final LoginAttemptService loginAttemptService;
	private final PasswordEncoder passwordEncoder;
	private final AuthMapper authMapper;
	private final Clock clock;

	public AuthService(
			AuthenticationManager authenticationManager,
			UserRepository userRepository,
			RefreshTokenRepository refreshTokenRepository,
			JwtTokenService jwtTokenService,
			RefreshTokenService refreshTokenService,
			LoginAttemptService loginAttemptService,
			PasswordEncoder passwordEncoder,
			AuthMapper authMapper,
			Clock clock) {
		this.authenticationManager = authenticationManager;
		this.userRepository = userRepository;
		this.refreshTokenRepository = refreshTokenRepository;
		this.jwtTokenService = jwtTokenService;
		this.refreshTokenService = refreshTokenService;
		this.loginAttemptService = loginAttemptService;
		this.passwordEncoder = passwordEncoder;
		this.authMapper = authMapper;
		this.clock = clock;
	}

	@Transactional
	public AuthResponse login(LoginRequest request, String remoteAddress, String deviceInfo) {
		loginAttemptService.assertAllowed(request.username(), remoteAddress);
		try {
			authenticationManager.authenticate(
					UsernamePasswordAuthenticationToken.unauthenticated(request.username(), request.password()));
		} catch (DisabledException exception) {
			loginAttemptService.recordFailure(request.username(), remoteAddress);
			throw new BadCredentialsException("Invalid username or password");
		} catch (AuthenticationException exception) {
			loginAttemptService.recordFailure(request.username(), remoteAddress);
			throw new BadCredentialsException("Invalid username or password");
		}

		UserAccount user = userRepository.findByUsername(request.username())
				.orElseThrow(() -> new BadCredentialsException("Invalid username or password"));
		Instant now = clock.instant();
		user.markLogin(now);
		loginAttemptService.recordSuccess(request.username(), remoteAddress);
		return issueTokens(user, now, deviceInfo);
	}

	@Transactional
	public AuthResponse refresh(RefreshTokenRequest request, String deviceInfo) {
		Instant now = clock.instant();
		RefreshToken refreshToken = refreshTokenRepository.findByTokenHash(refreshTokenService.hash(request.refreshToken()))
				.filter(token -> token.activeAt(now))
				.orElseThrow(() -> new BadCredentialsException("Invalid refresh token"));

		UserAccount user = refreshToken.getUser();
		if (user.isDisabled()) {
			refreshToken.revoke(now);
			throw new BadCredentialsException("Invalid refresh token");
		}

		refreshToken.revoke(now);
		return issueTokens(user, now, deviceInfo);
	}

	@Transactional
	public void logout(String refreshToken) {
		Instant now = clock.instant();
		refreshTokenRepository.findByTokenHash(refreshTokenService.hash(refreshToken))
				.filter(token -> token.getRevokedAt() == null)
				.ifPresent(token -> token.revoke(now));
	}

	@Transactional(readOnly = true)
	public CurrentUserResponse currentUser(String username) {
		UserAccount user = userRepository.findByUsername(username)
				.orElseThrow(() -> new BadCredentialsException("Invalid user"));
		if (user.isDisabled()) {
			throw new BadCredentialsException("Invalid user");
		}
		return authMapper.toCurrentUser(user);
	}

	@Transactional
	public CurrentUserResponse changePassword(String username, ChangePasswordRequest request) {
		UserAccount user = userRepository.findByUsername(username)
				.orElseThrow(() -> new BadCredentialsException("Invalid user"));
		if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
			throw new BadCredentialsException("Invalid username or password");
		}
		Instant now = clock.instant();
		user.changePassword(passwordEncoder.encode(request.newPassword()), now);
		refreshTokenRepository.revokeActiveTokensForUser(user, now);
		return authMapper.toCurrentUser(user);
	}

	private AuthResponse issueTokens(UserAccount user, Instant now, String deviceInfo) {
		AccessToken accessToken = jwtTokenService.issueAccessToken(user, now);
		IssuedRefreshToken refreshToken = refreshTokenService.issue(user, now, deviceInfo);
		return new AuthResponse(
				accessToken.token(),
				accessToken.expiresAt(),
				refreshToken.token(),
				refreshToken.expiresAt(),
				authMapper.toCurrentUser(user));
	}
}
