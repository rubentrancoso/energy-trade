package com.energytrade.orderservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.energytrade.orderservice.model.Order;

public interface OrderRepository extends JpaRepository<Order, Long> {
}
