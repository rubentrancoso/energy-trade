package com.energytrade.auditservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SpringBootApplication
public class AuditServiceApplication {
	
	private static final Logger logger = LoggerFactory.getLogger(AuditServiceApplication.class);

    public static void main(String[] args) {
        SpringApplication.run(AuditServiceApplication.class, args);
        logger.info("🚀 Audit Service is up and running! 🌐");
    }
}
