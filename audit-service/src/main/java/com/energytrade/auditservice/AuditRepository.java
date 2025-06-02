package com.energytrade.auditservice;

import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditRepository extends JpaRepository<AuditEvent, Long> {
}
