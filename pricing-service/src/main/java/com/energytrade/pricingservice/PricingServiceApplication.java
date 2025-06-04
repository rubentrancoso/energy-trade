package com.energytrade.pricingservice;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class PricingServiceApplication {
	
	private static final Logger logger = LoggerFactory.getLogger(PricingServiceApplication.class);
	
    public static void main(String[] args) {
        SpringApplication.run(PricingServiceApplication.class, args);
        logger.info("🚀 Pricing Service is up and running! 🌐");
    }
}
