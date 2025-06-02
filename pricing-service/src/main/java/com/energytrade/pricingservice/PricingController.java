package com.energytrade.pricingservice;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import com.energytrade.pricingservice.model.ExternalPriceResponse;
import com.energytrade.pricingservice.model.PriceResponse;


@RestController
public class PricingController {
	
	private static final Logger logger = LoggerFactory.getLogger(PricingController.class);

    @Value("${external.price.url}")
    private String externalPriceUrl;

    private final RestTemplate restTemplate = new RestTemplate();

    @GetMapping("/price")
    public PriceResponse getPrice() {
        ExternalPriceResponse external = restTemplate.getForObject(externalPriceUrl, ExternalPriceResponse.class);
        logger.info("💰 Calculated price for trade: {} {}", external.getValue(), external.getUnit());
        return new PriceResponse(external.getValue(), external.getUnit());
    }

}
