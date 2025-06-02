package com.energytrade.pricingservice.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ExternalPriceResponse {
    private double value;
    private String unit;
}