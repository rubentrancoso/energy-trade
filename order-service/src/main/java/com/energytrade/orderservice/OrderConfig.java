package com.energytrade.orderservice;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
@Data
public class OrderConfig {

    @Value("${audit.service.url}")
    private String auditServiceUrl;
}