package com.energytrade.orderservice;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class OrderServiceApplication {
	
	private static final Logger logger = LoggerFactory.getLogger(OrderServiceApplication.class);
	
    public static void main(String[] args) {
        SpringApplication.run(OrderServiceApplication.class, args);
        logger.info("🚀 Order Service is up and running! 🌐");
    }
}
