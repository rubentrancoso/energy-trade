package com.energytrade.orderservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.energytrade.orderservice.model.AuditFallback;

public interface AuditFallbackRepository extends JpaRepository<AuditFallback, Long> {
}