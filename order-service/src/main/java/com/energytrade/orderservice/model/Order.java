package com.energytrade.orderservice.model;

import java.time.OffsetDateTime;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "orders")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Order {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	// BUY or SELL
	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private OrderType type;

	// Target price set by the client
	@Column(nullable = false)
	private double price;

	// Total volume of the order (can be fractional, e.g., 1.5 MWh)
	@Column(nullable = false)
	private double volume;

	// Volume already executed (can be partial)
	@Builder.Default
	private double executedVolume = 0.0;

	// PENDING, PARTIAL, EXECUTED, CANCELLED
	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	@Builder.Default
	private OrderStatus status = OrderStatus.PENDING;

	// Order creation timestamp
	@Column(nullable = false)
	@Builder.Default
	private OffsetDateTime timestamp = OffsetDateTime.now();

	// Price retrieved from the pricing service at the time of order creation
	@Column(nullable = false)
	private double marketPrice;
	
	public double getRemainingVolume() {
	    return volume - executedVolume;
	}

}
