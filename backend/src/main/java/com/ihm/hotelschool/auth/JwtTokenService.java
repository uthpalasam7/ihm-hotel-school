package com.ihm.hotelschool.auth;

import com.ihm.hotelschool.branch.Branch;
import com.ihm.hotelschool.common.config.ApplicationProperties;
import com.ihm.hotelschool.user.Role;
import com.ihm.hotelschool.user.UserAccount;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.stereotype.Service;

@Service
public class JwtTokenService {

	private final JwtEncoder jwtEncoder;
	private final ApplicationProperties properties;

	public JwtTokenService(JwtEncoder jwtEncoder, ApplicationProperties properties) {
		this.jwtEncoder = jwtEncoder;
		this.properties = properties;
	}

	public AccessToken issueAccessToken(UserAccount user, Instant now) {
		Instant expiresAt = now.plus(properties.security().accessTokenMinutes(), ChronoUnit.MINUTES);
		List<String> roles = user.getRoles().stream().map(Role::getCode).sorted().toList();
		List<Long> branchIds = user.getBranches().stream().map(Branch::getId).sorted().toList();

		JwtClaimsSet claims = JwtClaimsSet.builder()
				.issuer("ihm-hotel-school")
				.issuedAt(now)
				.expiresAt(expiresAt)
				.subject(user.getUsername())
				.claim("userId", user.getId())
				.claim("roles", roles)
				.claim("branchIds", branchIds)
				.build();

		JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
		return new AccessToken(jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue(), expiresAt);
	}

	public record AccessToken(String token, Instant expiresAt) {
	}
}
