
package com.energytrade.orderservice;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.client.RestTemplate;

import com.energytrade.orderservice.model.Order;
import com.energytrade.orderservice.model.OrderStatus;
import com.energytrade.orderservice.model.OrderType;
import com.energytrade.orderservice.repository.OrderRepository;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class MatchingEngineTest {

    private OrderRepository orderRepository;
    private RestTemplate restTemplate;
    private MatchingEngine matchingEngine;
    
    @Value("${audit.service.url}")
    private String auditUrl;

    @BeforeEach
    public void setUp() {
        orderRepository = mock(OrderRepository.class);
        restTemplate = mock(RestTemplate.class);
        auditUrl = "http://mock-audit";
        matchingEngine = new MatchingEngine(orderRepository, restTemplate);
    }

    @Test
    public void shouldExecuteFullMatching() {
        Order buyOrder = Order.builder()
                .id(1L)
                .type(OrderType.BUY)
                .price(105.0)
                .volume(10.0)
                .executedVolume(0.0)
                .status(OrderStatus.PENDING)
                .build();

        Order sellOrder = Order.builder()
                .id(2L)
                .type(OrderType.SELL)
                .price(100.0)
                .volume(10.0)
                .executedVolume(0.0)
                .status(OrderStatus.PENDING)
                .build();

        when(orderRepository.findEligibleCounterpartOrders(OrderType.SELL, 105.0)).thenReturn(new ArrayList<>(List.of(sellOrder)));
        when(orderRepository.findById(1L)).thenReturn(Optional.of(buyOrder));

        matchingEngine.match(buyOrder);

        assertThat(buyOrder.getExecutedVolume()).isEqualTo(10.0);
        assertThat(buyOrder.getStatus()).isEqualTo(OrderStatus.EXECUTED);

        assertThat(sellOrder.getExecutedVolume()).isEqualTo(10.0);
        assertThat(sellOrder.getStatus()).isEqualTo(OrderStatus.EXECUTED);
    }

    @Test
    public void shouldExecutePartialMatching() {
        Order buyOrder = Order.builder()
                .id(1L)
                .type(OrderType.BUY)
                .price(105.0)
                .volume(10.0)
                .executedVolume(0.0)
                .status(OrderStatus.PENDING)
                .build();

        Order sellOrder = Order.builder()
                .id(2L)
                .type(OrderType.SELL)
                .price(100.0)
                .volume(6.0)
                .executedVolume(0.0)
                .status(OrderStatus.PENDING)
                .build();

        when(orderRepository.findEligibleCounterpartOrders(OrderType.SELL, 105.0)).thenReturn(new ArrayList<>(List.of(sellOrder)));
        when(orderRepository.findById(1L)).thenReturn(Optional.of(buyOrder));

        matchingEngine.match(buyOrder);

        assertThat(buyOrder.getExecutedVolume()).isEqualTo(6.0);
        assertThat(buyOrder.getStatus()).isEqualTo(OrderStatus.PARTIAL);

        assertThat(sellOrder.getExecutedVolume()).isEqualTo(6.0);
        assertThat(sellOrder.getStatus()).isEqualTo(OrderStatus.EXECUTED);
    }

    @Test
    public void shouldNotReexecuteCompletedOrder() {
        Order completedOrder = Order.builder()
                .id(1L)
                .type(OrderType.BUY)
                .price(100.0)
                .volume(10.0)
                .executedVolume(10.0)
                .status(OrderStatus.EXECUTED)
                .build();

        matchingEngine.match(completedOrder);

        // Should not trigger repository calls
        verify(orderRepository, never()).findEligibleCounterpartOrders(any(), anyDouble());
        verify(orderRepository, never()).save(any());
    }
    
    @Test
    void shouldIgnoreExecutedOrder() {
        Order executedOrder = new Order();
        executedOrder.setId(10L);
        executedOrder.setType(OrderType.BUY);
        executedOrder.setStatus(OrderStatus.EXECUTED); // Already processed
        executedOrder.setVolume(100);
        executedOrder.setPrice(120.0);

        MatchingEngine engine = new MatchingEngine(orderRepository, restTemplate);

        engine.match(executedOrder);

        // No interaction with repository should occur
        verify(orderRepository, never()).findEligibleCounterpartOrders(any(), anyDouble());
        verify(orderRepository, never()).save(any());
    }
    
    @Test
    void shouldIgnoreAlreadyExecutedOrder() {
        // given
        Order executedOrder = new Order();
        executedOrder.setId(99L);
        executedOrder.setType(OrderType.BUY);
        executedOrder.setStatus(OrderStatus.EXECUTED); // <- já foi executada
        executedOrder.setVolume(50.0);
        executedOrder.setPrice(105.0);

        // mock: lista vazia porque o método não deve nem ser chamado
        when(orderRepository.findEligibleCounterpartOrders(any(), anyDouble()))
            .thenReturn(new ArrayList<>()); // só por segurança

        // when
        matchingEngine.match(executedOrder);

        // then
        // assert that no counterpart lookup or saving occurred
        verify(orderRepository, never()).findEligibleCounterpartOrders(any(), anyDouble());
        verify(orderRepository, never()).save(any());

        // optionally check that status and volume remain unchanged
        assertEquals(OrderStatus.EXECUTED, executedOrder.getStatus());
        assertEquals(50.0, executedOrder.getVolume());
    }
    
    @Test
    void shouldExecuteBulkMatchingAgainstMultipleCounterparts() {
        Order buyOrder = Order.builder()
                .id(1L)
                .type(OrderType.BUY)
                .price(105.0)
                .volume(100.0)
                .executedVolume(0.0)
                .status(OrderStatus.PENDING)
                .timestamp(OffsetDateTime.now())
                .build();

        Order sell1 = Order.builder().id(2L).type(OrderType.SELL).price(100.0).volume(30.0).executedVolume(0.0).status(OrderStatus.PENDING).timestamp(OffsetDateTime.now()).build();
        Order sell2 = Order.builder().id(3L).type(OrderType.SELL).price(102.0).volume(50.0).executedVolume(0.0).status(OrderStatus.PENDING).timestamp(OffsetDateTime.now()).build();
        Order sell3 = Order.builder().id(4L).type(OrderType.SELL).price(103.0).volume(20.0).executedVolume(0.0).status(OrderStatus.PENDING).timestamp(OffsetDateTime.now()).build();

        when(orderRepository.findEligibleCounterpartOrders(OrderType.SELL, 105.0))
                .thenReturn(new ArrayList<>(List.of(sell1, sell2, sell3)));

        matchingEngine.match(buyOrder);

        assertEquals(OrderStatus.EXECUTED, buyOrder.getStatus());
        assertEquals(100.0, buyOrder.getExecutedVolume(), 0.001);
        assertEquals(30.0, sell1.getExecutedVolume(), 0.001);
        assertEquals(50.0, sell2.getExecutedVolume(), 0.001);
        assertEquals(20.0, sell3.getExecutedVolume(), 0.001);
    }



}
