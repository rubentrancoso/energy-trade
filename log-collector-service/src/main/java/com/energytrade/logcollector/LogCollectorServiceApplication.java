package com.energytrade.logcollector;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class LogCollectorServiceApplication {
	
	private static final Logger logger = LoggerFactory.getLogger(LogCollectorServiceApplication.class);

    public static void main(String[] args) {
        SpringApplication.run(LogCollectorServiceApplication.class, args);
        logger.info("🚀 Log Collector is up and running! 🌐");
    }
}
