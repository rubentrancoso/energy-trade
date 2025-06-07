package com.energytrade.orderservice;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import com.energytrade.orderservice.model.Order;
import com.energytrade.orderservice.model.OrderStatus;
import com.energytrade.orderservice.model.OrderType;
import com.energytrade.orderservice.repository.OrderRepository;

@ExtendWith(MockitoExtension.class)
class OrderControllerTest {

    @InjectMocks
    private OrderController controller;

    @Mock
    private OrderRepository repository;

    @Mock
    private RestTemplate restTemplate;

    @Value("${pricing.service.url}")
    private String pricingUrl = "http://mock-pricing";

    @Value("${audit.service.url}")
    private String auditUrl = "http://mock-audit";

    @Value("${notification.service.url}")
    private String notificationUrl = "http://mock-notify";

    @Mock
    private MatchingEngine matchingEngine;

    @Test
    void shouldCancelPendingOrderSuccessfully() {
        // Given
        Order order = Order.builder()
                .id(1L)
                .type(OrderType.BUY)
                .price(150.0)
                .volume(1.0)
                .executedVolume(0.0)
                .status(OrderStatus.PENDING)
                .marketPrice(150.0)
                .expirationTimestamp(OffsetDateTime.now().plusHours(1))
                .build();

        when(repository.findById(1L)).thenReturn(Optional.of(order));
        when(repository.save(any(Order.class))).thenReturn(order);

        // When
        ResponseEntity<?> response = controller.cancelOrder(1L);

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        Order cancelled = (Order) response.getBody();
        assertEquals(OrderStatus.CANCELLED, cancelled.getStatus());
        assertNotNull(cancelled.getCancelledAt());
    }
    
    @Test
    void shouldRejectCancellationOfExecutedOrder() {
        Order order = Order.builder()
                .id(2L)
                .status(OrderStatus.EXECUTED)
                .expirationTimestamp(OffsetDateTime.now().plusHours(1))
                .build();

        when(repository.findById(2L)).thenReturn(Optional.of(order));

        ResponseEntity<?> response = controller.cancelOrder(2L);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }
    
    @Test
    void shouldReturnNotFoundWhenCancellingNonexistentOrder() {
        when(repository.findById(999L)).thenReturn(Optional.empty());

        ResponseEntity<?> response = controller.cancelOrder(999L);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }
    
    @Test
    void shouldRejectCancellationOfAlreadyCancelledOrder() {
        Order order = Order.builder()
                .id(3L)
                .status(OrderStatus.CANCELLED)
                .expirationTimestamp(OffsetDateTime.now().plusHours(1))
                .build();

        when(repository.findById(3L)).thenReturn(Optional.of(order));

        ResponseEntity<?> response = controller.cancelOrder(3L);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }



}