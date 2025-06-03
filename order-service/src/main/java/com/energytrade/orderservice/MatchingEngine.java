package com.energytrade.orderservice;

import java.util.Comparator;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.energytrade.orderservice.model.AuditEvent;
import com.energytrade.orderservice.model.Notification;
import com.energytrade.orderservice.model.Order;
import com.energytrade.orderservice.model.OrderStatus;
import com.energytrade.orderservice.model.OrderType;
import com.energytrade.orderservice.repository.OrderRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class MatchingEngine {

    private final OrderRepository orderRepository;
    private final RestTemplate restTemplate;
    
    @Value("${audit.service.url}")
    private String auditUrl;
    
    @Value("${notification.service.url}")
    private String notificationUrl;

    public void match(Order incomingOrder) {
    	if (incomingOrder.getStatus() != OrderStatus.PENDING) {
    	    log.info("Order {} already processed. Skipping.", incomingOrder.getId());
    	    return;
    	}

        // Identify opposite order type
        OrderType oppositeType = (incomingOrder.getType() == OrderType.BUY)
                ? OrderType.SELL : OrderType.BUY;

        // Retrieve all potential counterpart orders
        List<Order> candidates = orderRepository.findEligibleCounterpartOrders(
                oppositeType, incomingOrder.getPrice()
        );

        // Sort by price-time priority
        candidates.sort(Comparator
                .comparing(Order::getPrice, incomingOrder.getType() == OrderType.BUY
                        ? Comparator.naturalOrder()  // Buy prefers lower sell prices
                        : Comparator.reverseOrder()) // Sell prefers higher buy prices
                .thenComparing(Order::getTimestamp));

        double remainingVolume = incomingOrder.getVolume();

        for (Order candidate : candidates) {
            double available = candidate.getVolume() - candidate.getExecutedVolume();
            if (available <= 0) continue;

            double traded = Math.min(remainingVolume, available);

            // Update candidate order
            candidate.setExecutedVolume(candidate.getExecutedVolume() + traded);
            if (candidate.getExecutedVolume() >= candidate.getVolume()) {
                candidate.setStatus(OrderStatus.EXECUTED);
            } else {
                candidate.setStatus(OrderStatus.PARTIAL);
            }

            // Update incoming order
            incomingOrder.setExecutedVolume(incomingOrder.getExecutedVolume() + traded);
            remainingVolume -= traded;
            
            
            AuditEvent auditEvent = new AuditEvent(
            	    "order-service",
            	    "ORDER_MATCHED",
            	    String.format("{\"orderId\":%d,\"status\":\"%s\",\"executedVolume\":%.2f}",
            	        incomingOrder.getId(),
            	        incomingOrder.getStatus(),
            	        incomingOrder.getExecutedVolume())
        	);
        	restTemplate.postForObject(auditUrl, auditEvent, Void.class);
            	
        	Notification notification = new Notification(
        		    "admin@energytrade.com",
        		    String.format("Order #%d partially/completely executed. New status: %s",
        		        incomingOrder.getId(),
        		        incomingOrder.getStatus())
    		);
    		restTemplate.postForObject(notificationUrl, notification, Void.class);

            if (remainingVolume <= 0.00001) break; // Fully matched

        }

        // Final status update for incoming order
        if (incomingOrder.getExecutedVolume() >= incomingOrder.getVolume()) {
            incomingOrder.setStatus(OrderStatus.EXECUTED);
        } else if (incomingOrder.getExecutedVolume() > 0) {
            incomingOrder.setStatus(OrderStatus.PARTIAL);
        }

        // Persist updates
        orderRepository.save(incomingOrder);
        orderRepository.saveAll(candidates);

        log.info("Matching completed for order id {}: executedVolume={}, status={}",
                incomingOrder.getId(),
                incomingOrder.getExecutedVolume(),
                incomingOrder.getStatus());
        
        AuditEvent executionEvent = new AuditEvent(
        	    "order-service",
        	    "ORDER_MATCHED",
        	    "{\"orderId\":" + incomingOrder.getId() +
        	    ",\"executedVolume\":" + incomingOrder.getExecutedVolume() +
        	    ",\"status\":\"" + incomingOrder.getStatus() + "\"}"
        	);
    	restTemplate.postForObject(auditUrl, executionEvent, Void.class);
    	
    	log.info("Matching completed for order #{} - final status: {}, executed: {}, remaining: {}",
    	        incomingOrder.getId(),
    	        incomingOrder.getStatus(),
    	        incomingOrder.getExecutedVolume(),
    	        incomingOrder.getRemainingVolume());

    }
}
