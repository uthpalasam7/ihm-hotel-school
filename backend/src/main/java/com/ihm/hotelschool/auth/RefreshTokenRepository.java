package com.ihm.hotelschool.auth;

import com.ihm.hotelschool.user.UserAccount;
import java.time.Instant;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {
	@EntityGraph(attributePaths = {"user", "user.roles", "user.branches"})
	Optional<RefreshToken> findByTokenHash(String tokenHash);

	@Modifying
	@Query("update RefreshToken token set token.revokedAt = :revokedAt where token.user = :user and token.revokedAt is null")
	void revokeActiveTokensForUser(UserAccount user, Instant revokedAt);
}
