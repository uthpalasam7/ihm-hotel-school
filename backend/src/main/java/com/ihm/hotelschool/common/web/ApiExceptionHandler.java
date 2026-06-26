package com.ihm.hotelschool.common.web;

import com.ihm.hotelschool.auth.AuthRateLimitException;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
class ApiExceptionHandler {

	@ExceptionHandler(MethodArgumentNotValidException.class)
	@ResponseStatus(HttpStatus.BAD_REQUEST)
	ApiError validation(MethodArgumentNotValidException exception, HttpServletRequest request) {
		List<ApiFieldError> fields = exception.getBindingResult().getFieldErrors().stream()
				.map(this::toFieldError)
				.toList();
		return error(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "Request validation failed", fields, request);
	}

	@ExceptionHandler({BadCredentialsException.class, AuthenticationException.class, JwtException.class})
	@ResponseStatus(HttpStatus.UNAUTHORIZED)
	ApiError authentication(RuntimeException exception, HttpServletRequest request) {
		return error(HttpStatus.UNAUTHORIZED, "AUTHENTICATION_FAILED", "Invalid username or password", List.of(), request);
	}

	@ExceptionHandler(AuthRateLimitException.class)
	@ResponseStatus(HttpStatus.TOO_MANY_REQUESTS)
	ApiError rateLimited(AuthRateLimitException exception, HttpServletRequest request) {
		return error(HttpStatus.TOO_MANY_REQUESTS, "RATE_LIMITED", exception.getMessage(), List.of(), request);
	}

	@ExceptionHandler({AccessDeniedException.class, AuthorizationDeniedException.class})
	@ResponseStatus(HttpStatus.FORBIDDEN)
	ApiError forbidden(RuntimeException exception, HttpServletRequest request) {
		return error(HttpStatus.FORBIDDEN, "ACCESS_DENIED", "Access denied", List.of(), request);
	}

	@ExceptionHandler(IllegalArgumentException.class)
	@ResponseStatus(HttpStatus.BAD_REQUEST)
	ApiError badRequest(IllegalArgumentException exception, HttpServletRequest request) {
		return error(HttpStatus.BAD_REQUEST, "BAD_REQUEST", exception.getMessage(), List.of(), request);
	}

	private ApiFieldError toFieldError(FieldError error) {
		return new ApiFieldError(error.getField(), error.getDefaultMessage());
	}

	private ApiError error(
			HttpStatus status,
			String code,
			String message,
			List<ApiFieldError> fieldErrors,
			HttpServletRequest request) {
		return new ApiError(Instant.now(), status.value(), code, message, fieldErrors, request.getRequestURI());
	}

	record ApiError(
			Instant timestamp,
			int status,
			String code,
			String message,
			List<ApiFieldError> fieldErrors,
			String path) {
	}

	record ApiFieldError(String field, String message) {
	}
}
