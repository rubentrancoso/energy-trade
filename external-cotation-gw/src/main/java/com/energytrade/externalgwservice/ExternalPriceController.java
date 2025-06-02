package com.energytrade.externalgwservice;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Random;

@RestController
public class ExternalPriceController {

    private final Random random = new Random();

    @GetMapping("/external-price")
    public ExternalPriceResponse getExternalPrice() {
        double price = 100 + (random.nextDouble() * 20); // entre 100 e 120
        return new ExternalPriceResponse(price, "USD/MWh");
    }
}