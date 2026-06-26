package com.ihm.hotelschool.auth;

import com.ihm.hotelschool.auth.dto.AuthResponse;
import com.ihm.hotelschool.auth.dto.ChangePasswordRequest;
import com.ihm.hotelschool.auth.dto.CurrentUserResponse;
import com.ihm.hotelschool.auth.dto.LoginRequest;
import com.ihm.hotelschool.auth.dto.LogoutRequest;
import com.ihm.hotelschool.auth.dto.RefreshTokenRequest;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.security.Principal;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

	private final AuthService authService;

	public AuthController(AuthService authService) {
		this.authService = authService;
	}

	@PostMapping("/login")
	public AuthResponse login(@Valid @RequestBody LoginRequest request, HttpServletRequest servletRequest) {
		return authService.login(request, servletRequest.getRemoteAddr(), servletRequest.getHeader("User-Agent"));
	}

	@PostMapping("/refresh")
	public AuthResponse refresh(@Valid @RequestBody RefreshTokenRequest request, HttpServletRequest servletRequest) {
		return authService.refresh(request, servletRequest.getHeader("User-Agent"));
	}

	@PostMapping("/logout")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void logout(@Valid @RequestBody LogoutRequest request) {
		authService.logout(request.refreshToken());
	}

	@GetMapping("/me")
	public CurrentUserResponse me(Principal principal) {
		return authService.currentUser(principal.getName());
	}

	@PostMapping("/change-password")
	public CurrentUserResponse changePassword(Principal principal, @Valid @RequestBody ChangePasswordRequest request) {
		return authService.changePassword(principal.getName(), request);
	}
}
