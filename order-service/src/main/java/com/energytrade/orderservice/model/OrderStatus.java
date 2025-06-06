package com.energytrade.orderservice.model;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum OrderStatus {
    PENDING,
    PARTIAL,
    EXECUTED,
    CANCELLED,
    EXPIRED;
	
    @JsonCreator
    public static OrderStatus from(String value) {
        return OrderStatus.valueOf(value.toUpperCase());
    }
}