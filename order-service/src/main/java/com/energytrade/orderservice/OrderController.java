package com.energytrade.orderservice;

import java.time.OffsetDateTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import com.energytrade.orderservice.model.AuditEvent;
import com.energytrade.orderservice.model.Notification;
import com.energytrade.orderservice.model.Order;
import com.energytrade.orderservice.model.OrderStatus;
import com.energytrade.orderservice.model.PriceResponse;
import com.energytrade.orderservice.repository.OrderRepository;

@RestController
@RequestMapping("/orders")
public class OrderController {

	private final OrderRepository repository;
	private final RestTemplate restTemplate;
	private final String pricingUrl;
	private final String auditUrl;
	private final String notificationUrl;
	private final MatchingEngine matchingEngine;

	public OrderController(OrderRepository repository, RestTemplate restTemplate,
			@Value("${pricing.service.url}") String pricingUrl, @Value("${audit.service.url}") String auditUrl,
			@Value("${notification.service.url}") String notificationUrl,
			MatchingEngine matchingEngine) {
		this.repository = repository;
		this.restTemplate = restTemplate;
		this.pricingUrl = pricingUrl;
		this.auditUrl = auditUrl;
		this.notificationUrl = notificationUrl;
		this.matchingEngine = matchingEngine;
	}

	@PostMapping
	public Order createOrder(@RequestBody Order order) {
		// Fetch current market price
		PriceResponse price = restTemplate.getForObject(pricingUrl, PriceResponse.class);
		order.setMarketPrice(price.getValue());

		// Initialize order state
		order.setExecutedVolume(0.0);
		order.setStatus(OrderStatus.PENDING);
		order.setTimestamp(OffsetDateTime.now());

		Order savedOrder = repository.save(order);
		matchingEngine.match(savedOrder);

		AuditEvent event = new AuditEvent("order-service", "ORDER_CREATED",
				"{\"orderId\":" + savedOrder.getId() + ",\"marketPrice\":" + savedOrder.getMarketPrice() + "}");
		restTemplate.postForObject(auditUrl, event, Void.class);

		Notification notification = new Notification("admin@energytrade.com",
				"Nova ordem criada com ID " + savedOrder.getId());
		restTemplate.postForObject(notificationUrl, notification, Void.class);

		return savedOrder;
	}

	@GetMapping
	public List<Order> listOrders() {
		return repository.findAll();
	}
}