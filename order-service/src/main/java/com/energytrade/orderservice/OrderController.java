package com.energytrade.orderservice;

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

    public OrderController(OrderRepository repository,
                           RestTemplate restTemplate,
                           @Value("${pricing.service.url}") String pricingUrl,
                           @Value("${audit.service.url}") String auditUrl, 
                           @Value("${notification.service.url}") String notificationUrl) {
        this.repository = repository;
        this.restTemplate = restTemplate;
        this.pricingUrl = pricingUrl;
        this.auditUrl = auditUrl;
        this.notificationUrl = notificationUrl;
    }

    @PostMapping
    public Order createOrder(@RequestBody Order order) {
        PriceResponse price = restTemplate.getForObject(pricingUrl, PriceResponse.class);
        order.setMarketPrice(price.getValue());

        Order savedOrder = repository.save(order);

        AuditEvent event = new AuditEvent(
                "order-service",
                "ORDER_CREATED",
                "{\"orderId\":" + savedOrder.getId() + ",\"marketPrice\":" + savedOrder.getMarketPrice() + "}"
        );
        restTemplate.postForObject(auditUrl, event, Void.class);
        
        Notification notification = new Notification(
        	    "admin@energytrade.com",
        	    "Nova ordem criada com ID " + savedOrder.getId()
        	);
        	restTemplate.postForObject(notificationUrl, notification, Void.class);


        return savedOrder;
    }

    @GetMapping
    public List<Order> listOrders() {
        return repository.findAll();
    }
}