package com.ihm.hotelschool.audit;

import com.fasterxml.jackson.databind.JsonNode;
import com.ihm.hotelschool.branch.Branch;
import com.ihm.hotelschool.user.UserAccount;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "audit_logs")
public class AuditLog {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "actor_user_id")
	private UserAccount actor;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "branch_id")
	private Branch branch;

	@Column(nullable = false, length = 100)
	private String action;

	@Column(name = "entity_type", nullable = false, length = 100)
	private String entityType;

	@Column(name = "entity_id", nullable = false, length = 100)
	private String entityId;

	private String reason;

	@JdbcTypeCode(SqlTypes.JSON)
	@Column(name = "old_value_json", columnDefinition = "jsonb")
	private JsonNode oldValueJson;

	@JdbcTypeCode(SqlTypes.JSON)
	@Column(name = "new_value_json", columnDefinition = "jsonb")
	private JsonNode newValueJson;

	@Column(name = "request_id", length = 100)
	private String requestId;

	@Column(name = "ip_address", length = 100)
	private String ipAddress;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	protected AuditLog() {
	}

	public AuditLog(
			UserAccount actor,
			Branch branch,
			String action,
			String entityType,
			String entityId,
			String reason,
			JsonNode oldValueJson,
			JsonNode newValueJson,
			String requestId,
			String ipAddress,
			Instant createdAt) {
		this.actor = actor;
		this.branch = branch;
		this.action = action;
		this.entityType = entityType;
		this.entityId = entityId;
		this.reason = reason;
		this.oldValueJson = oldValueJson;
		this.newValueJson = newValueJson;
		this.requestId = requestId;
		this.ipAddress = ipAddress;
		this.createdAt = createdAt;
	}
}
