package com.ihm.hotelschool.auth;

import com.ihm.hotelschool.user.UserAccount;
import com.ihm.hotelschool.user.UserStatus;
import java.util.Collection;
import java.util.List;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

public class AuthUser implements UserDetails {

	private final Long id;
	private final String username;
	private final String passwordHash;
	private final UserStatus status;
	private final List<GrantedAuthority> authorities;

	public AuthUser(UserAccount user) {
		this.id = user.getId();
		this.username = user.getUsername();
		this.passwordHash = user.getPasswordHash();
		this.status = user.getStatus();
		this.authorities = user.getRoles().stream()
				.map(role -> new SimpleGrantedAuthority("ROLE_" + role.getCode()))
				.map(GrantedAuthority.class::cast)
				.toList();
	}

	public Long id() {
		return id;
	}

	@Override
	public Collection<? extends GrantedAuthority> getAuthorities() {
		return authorities;
	}

	@Override
	public String getPassword() {
		return passwordHash;
	}

	@Override
	public String getUsername() {
		return username;
	}

	@Override
	public boolean isEnabled() {
		return status != UserStatus.DISABLED;
	}
}
