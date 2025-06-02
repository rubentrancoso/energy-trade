package com.energytrade.auditservice;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/audit")
public class AuditController {

	private static final Logger logger = LoggerFactory.getLogger(AuditController.class);
	
    private final AuditRepository repository;

    public AuditController(AuditRepository repository) {
        this.repository = repository;
    }

    @PostMapping
    public AuditEvent register(@RequestBody AuditEvent event) {
    	logger.info("🔍 Audit event received: {}", event);
        return repository.save(event);
    }
}
