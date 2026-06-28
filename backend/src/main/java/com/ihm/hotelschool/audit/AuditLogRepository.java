package com.ihm.hotelschool.audit;

import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
	long countByActionAndEntityTypeAndEntityId(String action, String entityType, String entityId);
}
