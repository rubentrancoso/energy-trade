package com.energytrade.notificationservice;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class NotificationServiceApplication {
	
	private static final Logger logger = LoggerFactory.getLogger(NotificationServiceApplication.class);
	
    public static void main(String[] args) {
        SpringApplication.run(NotificationServiceApplication.class, args);
        logger.info("🚀 Log Collector is up and running! 🌐");
    }
}
