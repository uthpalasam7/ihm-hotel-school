package com.ihm.hotelschool.audit;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ihm.hotelschool.branch.Branch;
import com.ihm.hotelschool.user.UserAccount;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Clock;
import org.springframework.stereotype.Service;

@Service
public class AuditService {

	private final AuditLogRepository auditLogRepository;
	private final ObjectMapper objectMapper;
	private final Clock clock;

	public AuditService(AuditLogRepository auditLogRepository, ObjectMapper objectMapper, Clock clock) {
		this.auditLogRepository = auditLogRepository;
		this.objectMapper = objectMapper;
		this.clock = clock;
	}

	public void record(
			UserAccount actor,
			Branch branch,
			String action,
			String entityType,
			Object entityId,
			Object oldValue,
			Object newValue,
			String reason,
			HttpServletRequest request) {
		auditLogRepository.save(new AuditLog(
				actor,
				branch,
				action,
				entityType,
				String.valueOf(entityId),
				blankToNull(reason),
				toJsonNode(oldValue),
				toJsonNode(newValue),
				request == null ? null : request.getHeader("X-Request-Id"),
				request == null ? null : request.getRemoteAddr(),
				clock.instant()));
	}

	private JsonNode toJsonNode(Object value) {
		if (value == null) {
			return null;
		}
		return objectMapper.valueToTree(value);
	}

	private String blankToNull(String value) {
		return value == null || value.isBlank() ? null : value.trim();
	}
}
