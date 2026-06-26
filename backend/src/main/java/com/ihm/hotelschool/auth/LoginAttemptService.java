package com.ihm.hotelschool.auth;

import com.ihm.hotelschool.common.config.ApplicationProperties;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.springframework.stereotype.Service;

@Service
class LoginAttemptService {

	private final ConcurrentMap<String, AttemptState> attempts = new ConcurrentHashMap<>();
	private final ApplicationProperties properties;
	private final Clock clock;

	LoginAttemptService(ApplicationProperties properties, Clock clock) {
		this.properties = properties;
		this.clock = clock;
	}

	void assertAllowed(String username, String remoteAddress) {
		AttemptState state = attempts.get(key(username, remoteAddress));
		if (state != null && state.blockedAt(clock.instant())) {
			throw new AuthRateLimitException();
		}
	}

	void recordFailure(String username, String remoteAddress) {
		Instant now = clock.instant();
		AttemptState state = attempts.compute(key(username, remoteAddress), (_key, current) -> {
			if (current == null || current.expiredAt(now)) {
				return AttemptState.failedOnce(now);
			}
			return current.failedAgain(now, properties.security().maxFailedLoginAttempts(), properties.security().failedLoginLockMinutes());
		});
		if (state.blockedAt(now)) {
			throw new AuthRateLimitException();
		}
	}

	void recordSuccess(String username, String remoteAddress) {
		attempts.remove(key(username, remoteAddress));
	}

	private String key(String username, String remoteAddress) {
		String normalizedUsername = username == null ? "" : username.trim().toLowerCase(Locale.ROOT);
		String normalizedAddress = remoteAddress == null || remoteAddress.isBlank() ? "unknown" : remoteAddress.trim();
		return normalizedUsername + "|" + normalizedAddress;
	}

	private record AttemptState(int failedAttempts, Instant lastFailedAt, Instant blockedUntil) {

		static AttemptState failedOnce(Instant now) {
			return new AttemptState(1, now, null);
		}

		AttemptState failedAgain(Instant now, int maxFailedAttempts, long lockMinutes) {
			int nextFailures = failedAttempts + 1;
			Instant nextBlockedUntil = nextFailures >= maxFailedAttempts ? now.plus(lockMinutes, ChronoUnit.MINUTES) : blockedUntil;
			return new AttemptState(nextFailures, now, nextBlockedUntil);
		}

		boolean blockedAt(Instant now) {
			return blockedUntil != null && blockedUntil.isAfter(now);
		}

		boolean expiredAt(Instant now) {
			return blockedUntil != null && !blockedUntil.isAfter(now);
		}
	}
}
