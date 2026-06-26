package com.ihm.hotelschool.auth;

public class AuthRateLimitException extends RuntimeException {

	public AuthRateLimitException() {
		super("Too many failed login attempts. Try again later.");
	}
}
