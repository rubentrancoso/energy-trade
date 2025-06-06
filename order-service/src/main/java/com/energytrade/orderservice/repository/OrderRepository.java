package com.energytrade.orderservice.repository;

import java.time.OffsetDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.energytrade.orderservice.model.Order;
import com.energytrade.orderservice.model.OrderStatus;
import com.energytrade.orderservice.model.OrderType;

public interface OrderRepository extends JpaRepository<Order, Long> {

	@Query("SELECT o FROM Order o WHERE o.type = :type AND o.price <= :price")
	List<Order> findEligibleCounterpartOrders(@Param("type") OrderType type, @Param("price") Double price);
	
	List<Order> findByStatusAndExpirationTimestampBefore(OrderStatus status, OffsetDateTime cutoff);

}
