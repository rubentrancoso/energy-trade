package com.energytrade.integrationsim;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public  class Order {
    private String type;
    private Double price;
    private Double volume;
    private Long id;
    private Double marketPrice;

    public Order(String type, Double price, Double volume) {
        this.type = type;
        this.price = price;
        this.volume = volume;
    }
}
