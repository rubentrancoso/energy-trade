package com.energytrade.orderservice;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Locale;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
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

	private static final Logger logger = LoggerFactory.getLogger(OrderController.class);

	private final OrderRepository repository;
	private final RestTemplate restTemplate;
	private final String pricingUrl;
	private final String auditUrl;
	private final String notificationUrl;
	private final MatchingEngine matchingEngine;

	public OrderController(OrderRepository repository, RestTemplate restTemplate,
			@Value("${pricing.service.url}") String pricingUrl, @Value("${audit.service.url}") String auditUrl,
			@Value("${notification.service.url}") String notificationUrl, MatchingEngine matchingEngine) {
		this.repository = repository;
		this.restTemplate = restTemplate;
		this.pricingUrl = pricingUrl;
		this.auditUrl = auditUrl;
		this.notificationUrl = notificationUrl;
		this.matchingEngine = matchingEngine;
	}

	@PostMapping
	public ResponseEntity<?> createOrder(@RequestBody Order order) {
		// Validate volume before proceeding
		if (order.getVolume() <= 0) {
			logger.warn("❌ Rejected order with invalid volume: {}", order);
			return ResponseEntity.badRequest().body("Order volume must be a positive number.");
		}

		// Fetch current market price
		PriceResponse price = restTemplate.getForObject(pricingUrl, PriceResponse.class);
		order.setMarketPrice(price.getValue());

		// Initialize order state
		order.setExecutedVolume(0.0);
		order.setStatus(OrderStatus.PENDING);
		order.setTimestamp(OffsetDateTime.now());

		Order savedOrder = repository.save(order);
		matchingEngine.match(savedOrder);

		AuditEvent event = new AuditEvent("order-service", "ORDER_CREATED", String.format(Locale.US,
				"{\"orderId\":%d,\"marketPrice\":%.2f}", savedOrder.getId(), savedOrder.getMarketPrice()));
		restTemplate.postForObject(auditUrl, event, Void.class); // ✅ Envio do evento de auditoria

		Notification notification = new Notification("admin@energytrade.com",
				"Nova ordem criada com ID " + savedOrder.getId());
		restTemplate.postForObject(notificationUrl, notification, Void.class);

		return ResponseEntity.ok(savedOrder);
	}

	@DeleteMapping("/{orderId}")
	public ResponseEntity<?> cancelOrder(@PathVariable Long orderId) {
		return repository.findById(orderId).map(order -> {
			// Only allow canceling if order is still pending or partial
			if (order.getStatus() == OrderStatus.EXECUTED || order.getStatus() == OrderStatus.CANCELLED) {
				logger.warn("⚠️ Attempted to cancel order in invalid state: {}", order);
				return ResponseEntity.badRequest()
						.body("Cannot cancel an order that is already executed or cancelled.");
			}

			order.setStatus(OrderStatus.CANCELLED);
			order.setCancelledAt(OffsetDateTime.now());

			repository.save(order);

			// Audit logging
			AuditEvent event = new AuditEvent("order-service", "ORDER_CANCELLED",
					String.format(Locale.US, "{\"orderId\":%d}", order.getId()));
			restTemplate.postForObject(auditUrl, event, Void.class);

			// Optional: notify admin
			Notification notification = new Notification("admin@energytrade.com",
					"Ordem cancelada com ID " + order.getId());
			restTemplate.postForObject(notificationUrl, notification, Void.class);

			logger.info("✅ Order {} successfully cancelled", orderId);
			return ResponseEntity.ok(order);
		}).orElseGet(() -> {
			logger.warn("❌ Cancel attempt failed: Order {} not found", orderId);
			return ResponseEntity.notFound().build();
		});
	}
	
	@GetMapping("/{id}")
	public ResponseEntity<?> getOrderById(@PathVariable Long id) {
	    return repository.findById(id)
	            .map(ResponseEntity::ok)
	            .orElseGet(() -> ResponseEntity.notFound().build());
	}

	@GetMapping
	public List<Order> listOrders() {
		return repository.findAll();
	}
}