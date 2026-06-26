package com.ihm.hotelschool.common.web;

import com.ihm.hotelschool.common.config.ApplicationProperties;
import java.time.Instant;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/health")
class HealthController {

	private final ApplicationProperties applicationProperties;

	HealthController(ApplicationProperties applicationProperties) {
		this.applicationProperties = applicationProperties;
	}

	@GetMapping
	HealthResponse health() {
		return new HealthResponse(
				"UP",
				"IHM Hotel School Management System",
				applicationProperties.timezone(),
				applicationProperties.currencyCode(),
				Instant.now());
	}

	record HealthResponse(String status, String service, String timezone, String currencyCode, Instant timestamp) {
	}
}
