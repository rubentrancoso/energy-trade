package com.energytrade.externalgwservice;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class ExternalCotationGwApplication {
	
	private static final Logger logger = LoggerFactory.getLogger(ExternalCotationGwApplication.class);
	
    public static void main(String[] args) {
        SpringApplication.run(ExternalCotationGwApplication.class, args);
        logger.info("🚀 External Cotation GW is up and running! 🌐");
    }
}
