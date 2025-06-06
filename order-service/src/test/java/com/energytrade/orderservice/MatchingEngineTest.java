
package com.energytrade.orderservice;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.web.client.RestTemplate;

import com.energytrade.orderservice.model.AuditEvent;
import com.energytrade.orderservice.model.Notification;
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

	@BeforeEach
	public void setUp() {
		orderRepository = mock(OrderRepository.class);
		restTemplate = mock(RestTemplate.class);
		matchingEngine = new MatchingEngine(orderRepository, restTemplate);
	}


	@Test
	public void shouldExecuteFullMatching() {
	    OffsetDateTime now = OffsetDateTime.now();

	    Order buyOrder = Order.builder()
	            .id(1L)
	            .type(OrderType.BUY)
	            .price(105.0)
	            .volume(10.0)
	            .executedVolume(0.0)
	            .status(OrderStatus.PENDING)
	            .marketPrice(103.0)
	            .timestamp(now)
	            .expirationTimestamp(now.plusHours(1))
	            .build();

	    Order sellOrder = Order.builder()
	            .id(2L)
	            .type(OrderType.SELL)
	            .price(100.0)
	            .volume(10.0)
	            .executedVolume(0.0)
	            .status(OrderStatus.PENDING)
	            .marketPrice(99.0)
	            .timestamp(now)
	            .expirationTimestamp(now.plusHours(1))
	            .build();

	    when(orderRepository.findEligibleCounterpartOrders(OrderType.SELL, 105.0))
	            .thenReturn(List.of(sellOrder));

	    // 👉 injeção via reflexão
	    setField(matchingEngine, "auditUrl", "http://mock-audit");
	    setField(matchingEngine, "notificationUrl", "http://mock-notification");

	    matchingEngine.match(buyOrder);

	    assertThat(buyOrder.getStatus()).isEqualTo(OrderStatus.EXECUTED);
	    assertThat(sellOrder.getStatus()).isEqualTo(OrderStatus.EXECUTED);

	    // 🎯 Captura múltiplas chamadas com AuditEvent
	    ArgumentCaptor<AuditEvent> auditCaptor = ArgumentCaptor.forClass(AuditEvent.class);
	    verify(restTemplate, org.mockito.Mockito.times(2))
	            .postForObject(eq("http://mock-audit"), auditCaptor.capture(), eq(Void.class));

	    List<AuditEvent> auditEvents = auditCaptor.getAllValues();
	    assertThat(auditEvents).hasSize(2);

	 // ✅ Verificação mais robusta, ignora ordem dos eventos
	    assertThat(auditEvents.stream().map(AuditEvent::getType))
	        .containsExactlyInAnyOrder("ORDER_EXECUTED_PAIRWISE", "ORDER_MATCHED");

	    // Validando conteúdo específico dos payloads
	    boolean hasPairwiseWithMaker = auditEvents.stream()
	        .filter(e -> "ORDER_EXECUTED_PAIRWISE".equals(e.getType()))
	        .anyMatch(e -> e.getPayload().contains("\"makerOrderId\":2"));

	    boolean hasMatchWithTaker = auditEvents.stream()
	        .filter(e -> "ORDER_MATCHED".equals(e.getType()))
	        .anyMatch(e -> e.getPayload().contains("\"orderId\":1"));

	    assertThat(hasPairwiseWithMaker).isTrue();
	    assertThat(hasMatchWithTaker).isTrue();

	    // 🎯 Verifica notificação enviada
	    ArgumentCaptor<Notification> notificationCaptor = ArgumentCaptor.forClass(Notification.class);
	    verify(restTemplate).postForObject(eq("http://mock-notification"), notificationCaptor.capture(), eq(Void.class));
	    Notification notif = notificationCaptor.getValue();
	    assertThat(notif.getMessage()).contains("matched with #2");
	}

	
	private void setField(Object target, String fieldName, Object value) {
	    try {
	        var field = target.getClass().getDeclaredField(fieldName);
	        field.setAccessible(true);
	        field.set(target, value);
	    } catch (Exception e) {
	        throw new RuntimeException(e);
	    }
	}




	@Test
	public void shouldExecutePartialMatching() {
	    OffsetDateTime now = OffsetDateTime.now();

	    Order buyOrder = Order.builder()
	            .id(1L)
	            .type(OrderType.BUY)
	            .price(105.0)
	            .volume(10.0)
	            .executedVolume(0.0)
	            .status(OrderStatus.PENDING)
	            .marketPrice(104.0)
	            .timestamp(now.minusMinutes(2))
	            .expirationTimestamp(now.plusMinutes(10))
	            .build();

	    Order sellOrder = Order.builder()
	            .id(2L)
	            .type(OrderType.SELL)
	            .price(100.0)
	            .volume(6.0)
	            .executedVolume(0.0)
	            .status(OrderStatus.PENDING)
	            .marketPrice(99.0)
	            .timestamp(now.minusMinutes(5))
	            .expirationTimestamp(now.plusMinutes(10))
	            .build();

	    when(orderRepository.findEligibleCounterpartOrders(OrderType.SELL, 105.0))
	            .thenReturn(new ArrayList<>(List.of(sellOrder)));

	    matchingEngine.match(buyOrder);

	    assertThat(buyOrder.getExecutedVolume()).isEqualTo(6.0);
	    assertThat(buyOrder.getStatus()).isEqualTo(OrderStatus.PARTIAL);
	    assertEquals(4.0, buyOrder.getRemainingVolume(), 0.001); // ✅ nova verificação

	    assertThat(sellOrder.getExecutedVolume()).isEqualTo(6.0);
	    assertThat(sellOrder.getStatus()).isEqualTo(OrderStatus.EXECUTED);
	}

	@Test
	void shouldSkipProcessingOfAlreadyExecutedOrder() {
	    OffsetDateTime now = OffsetDateTime.now();

	    Order executedOrder = Order.builder()
	            .id(99L)
	            .type(OrderType.BUY)
	            .status(OrderStatus.EXECUTED) // Já foi executada
	            .volume(50.0)
	            .executedVolume(50.0)
	            .price(105.0)
	            .marketPrice(104.0)
	            .timestamp(now.minusMinutes(10))
	            .expirationTimestamp(now.plusMinutes(10))
	            .build();

	    // Segurança: se a lógica estiver errada, não deve buscar contrapartes
	    when(orderRepository.findEligibleCounterpartOrders(any(), anyDouble()))
	        .thenReturn(List.of());

	    matchingEngine.match(executedOrder);

	    // Verifica que nenhum método relevante foi invocado
	    verify(orderRepository, never()).findEligibleCounterpartOrders(any(), anyDouble());
	    verify(orderRepository, never()).save(any());
	    verify(orderRepository, never()).saveAll(any());

	    // Confirma que o estado permanece o mesmo
	    assertEquals(OrderStatus.EXECUTED, executedOrder.getStatus());
	    assertEquals(50.0, executedOrder.getExecutedVolume(), 0.001);
	    assertEquals(0.0, executedOrder.getRemainingVolume(), 0.001);
	}


	@Test
	void shouldExecuteBulkMatchingAgainstMultipleCounterparts() {
		OffsetDateTime now = OffsetDateTime.now();

		Order buyOrder = Order.builder().id(1L).type(OrderType.BUY).price(105.0).volume(100.0).executedVolume(0.0)
				.status(OrderStatus.PENDING).timestamp(now.minusMinutes(2)).marketPrice(104.0)
				.expirationTimestamp(now.plusHours(1)).build();

		Order sell1 = Order.builder().id(2L).type(OrderType.SELL).price(100.0).volume(30.0).executedVolume(0.0)
				.status(OrderStatus.PENDING).timestamp(now.minusMinutes(5)).marketPrice(99.0)
				.expirationTimestamp(now.plusHours(1)).build();

		Order sell2 = Order.builder().id(3L).type(OrderType.SELL).price(102.0).volume(50.0).executedVolume(0.0)
				.status(OrderStatus.PENDING).timestamp(now.minusMinutes(4)).marketPrice(101.0)
				.expirationTimestamp(now.plusHours(1)).build();

		Order sell3 = Order.builder().id(4L).type(OrderType.SELL).price(103.0).volume(20.0).executedVolume(0.0)
				.status(OrderStatus.PENDING).timestamp(now.minusMinutes(3)).marketPrice(102.0)
				.expirationTimestamp(now.plusHours(1)).build();

		when(orderRepository.findEligibleCounterpartOrders(OrderType.SELL, 105.0))
				.thenReturn(new ArrayList<>(List.of(sell1, sell2, sell3)));

		matchingEngine.match(buyOrder);

		assertEquals(OrderStatus.EXECUTED, buyOrder.getStatus());
		assertEquals(100.0, buyOrder.getExecutedVolume(), 0.001);
		assertEquals(30.0, sell1.getExecutedVolume(), 0.001);
		assertEquals(50.0, sell2.getExecutedVolume(), 0.001);
		assertEquals(20.0, sell3.getExecutedVolume(), 0.001);
		
		assertEquals(OrderStatus.EXECUTED, sell1.getStatus());
		assertEquals(OrderStatus.EXECUTED, sell2.getStatus());
		assertEquals(OrderStatus.EXECUTED, sell3.getStatus());

	}

	@Test
	public void shouldIgnoreExpiredOrders() {
	    OffsetDateTime now = OffsetDateTime.now();

	    // Incoming valid BUY order
	    Order incomingOrder = Order.builder()
	            .id(1L)
	            .type(OrderType.BUY)
	            .price(120.0)
	            .volume(10.0)
	            .executedVolume(0.0)
	            .status(OrderStatus.PENDING)
	            .timestamp(now.minusMinutes(1))
	            .marketPrice(118.0)
	            .expirationTimestamp(now.plusMinutes(10)) // Ainda válida
	            .build();

	    // Expired candidate 1
	    Order expiredSell1 = Order.builder()
	            .id(2L)
	            .type(OrderType.SELL)
	            .price(100.0)
	            .volume(10.0)
	            .executedVolume(0.0)
	            .status(OrderStatus.PENDING)
	            .timestamp(now.minusMinutes(5))
	            .marketPrice(98.0)
	            .expirationTimestamp(now.minusMinutes(1)) // Expirada
	            .build();

	    // Expired candidate 2
	    Order expiredSell2 = Order.builder()
	            .id(3L)
	            .type(OrderType.SELL)
	            .price(99.0)
	            .volume(5.0)
	            .executedVolume(0.0)
	            .status(OrderStatus.PENDING)
	            .timestamp(now.minusMinutes(6))
	            .marketPrice(97.0)
	            .expirationTimestamp(now.minusMinutes(2)) // Expirada
	            .build();

	    // Valid candidate
	    Order validSell = Order.builder()
	            .id(4L)
	            .type(OrderType.SELL)
	            .price(100.0)
	            .volume(10.0)
	            .executedVolume(0.0)
	            .status(OrderStatus.PENDING)
	            .timestamp(now.minusMinutes(3))
	            .marketPrice(99.0)
	            .expirationTimestamp(now.plusMinutes(5)) // Ainda válida
	            .build();

	    List<Order> candidates = Arrays.asList(expiredSell1, expiredSell2, validSell);
	    when(orderRepository.findEligibleCounterpartOrders(eq(OrderType.SELL), anyDouble()))
	            .thenReturn(candidates);

	    matchingEngine.match(incomingOrder);

	    assertEquals(OrderStatus.EXECUTED, incomingOrder.getStatus());
	    assertEquals(10.0, incomingOrder.getExecutedVolume(), 0.001);

	    assertEquals(OrderStatus.EXECUTED, validSell.getStatus());
	    assertEquals(10.0, validSell.getExecutedVolume(), 0.001);

	    assertEquals(OrderStatus.PENDING, expiredSell1.getStatus());
	    assertEquals(0.0, expiredSell1.getExecutedVolume(), 0.001);

	    assertEquals(OrderStatus.PENDING, expiredSell2.getStatus());
	    assertEquals(0.0, expiredSell2.getExecutedVolume(), 0.001);
	}
	
	@Test
	void shouldRejectExpiredIncomingOrder() {
	    OffsetDateTime now = OffsetDateTime.now();

	    Order expiredBuy = Order.builder()
	            .id(99L)
	            .type(OrderType.BUY)
	            .status(OrderStatus.PENDING)
	            .volume(10.0)
	            .executedVolume(0.0)
	            .price(100.0)
	            .marketPrice(98.0)
	            .timestamp(now.minusMinutes(10))
	            .expirationTimestamp(now.minusMinutes(1)) // Já expirado
	            .build();

	    matchingEngine.match(expiredBuy);

	    assertEquals(OrderStatus.EXPIRED, expiredBuy.getStatus());

	    // Deve salvar apenas a própria ordem expirada
	    verify(orderRepository).save(expiredBuy);
	    verify(orderRepository, never()).findEligibleCounterpartOrders(any(), anyDouble());
	    verify(orderRepository, never()).saveAll(any()); // nenhuma contraparte foi salva
	}
	
	@Test
	void shouldLeaveCounterpartAsPartialIfVolumeRemaining() {
	    OffsetDateTime now = OffsetDateTime.now();

	    Order buyOrder = Order.builder()
	            .id(1L)
	            .type(OrderType.BUY)
	            .price(105.0)
	            .volume(10.0)
	            .executedVolume(0.0)
	            .status(OrderStatus.PENDING)
	            .timestamp(now.minusMinutes(1))
	            .marketPrice(104.0)
	            .expirationTimestamp(now.plusMinutes(10))
	            .build();

	    Order sellOrder = Order.builder()
	            .id(2L)
	            .type(OrderType.SELL)
	            .price(100.0)
	            .volume(15.0)
	            .executedVolume(0.0)
	            .status(OrderStatus.PENDING)
	            .timestamp(now.minusMinutes(3))
	            .marketPrice(99.0)
	            .expirationTimestamp(now.plusMinutes(10))
	            .build();

	    when(orderRepository.findEligibleCounterpartOrders(OrderType.SELL, 105.0))
	            .thenReturn(List.of(sellOrder));

	    matchingEngine.match(buyOrder);

	    // BUY foi totalmente executada
	    assertEquals(OrderStatus.EXECUTED, buyOrder.getStatus());
	    assertEquals(10.0, buyOrder.getExecutedVolume(), 0.001);

	    // SELL foi parcialmente executada (10 de 15)
	    assertEquals(OrderStatus.PARTIAL, sellOrder.getStatus());
	    assertEquals(10.0, sellOrder.getExecutedVolume(), 0.001);
	    assertEquals(5.0, sellOrder.getRemainingVolume(), 0.001); // 15 - 10
	}

	@Test
	void shouldPrioritizeCheapestAndOldestSellOrders() {
	    OffsetDateTime now = OffsetDateTime.now();

	    // Criando uma ordem de compra (BUY) com volume total de 10
	    Order buy = Order.builder()
	            .id(10L)
	            .type(OrderType.BUY)
	            .price(105.0)
	            .volume(10.0)
	            .executedVolume(0.0)
	            .status(OrderStatus.PENDING)
	            .marketPrice(104.0)
	            .timestamp(now)
	            .expirationTimestamp(now.plusHours(1))
	            .build();

	    // Ordem SELL mais recente (deve ser ignorada inicialmente)
	    Order sellRecent = Order.builder()
	            .id(1L)
	            .type(OrderType.SELL)
	            .price(100.0)
	            .volume(10.0)
	            .executedVolume(0.0)
	            .status(OrderStatus.PENDING)
	            .timestamp(now.minusMinutes(1)) // mais recente
	            .marketPrice(99.0)
	            .expirationTimestamp(now.plusHours(1))
	            .build();

	    // Ordem SELL mais antiga (deve ser priorizada)
	    Order sellOld = Order.builder()
	            .id(2L)
	            .type(OrderType.SELL)
	            .price(100.0)
	            .volume(10.0)
	            .executedVolume(0.0)
	            .status(OrderStatus.PENDING)
	            .timestamp(now.minusMinutes(5)) // mais antiga
	            .marketPrice(99.0)
	            .expirationTimestamp(now.plusHours(1))
	            .build();

	    // Mock do repositório retornando as ordens fora de ordem proposital
	    when(orderRepository.findEligibleCounterpartOrders(OrderType.SELL, 105.0))
	            .thenReturn(List.of(sellRecent, sellOld));

	    matchingEngine.match(buy);

	    // ✅ Verifica se a ordem mais antiga (sellOld) foi realmente executada
	    assertThat(sellOld.getExecutedVolume()).isEqualTo(10.0);
	    assertThat(sellRecent.getExecutedVolume()).isEqualTo(0.0);

	    // ✅ Verifica se a lista passada ao saveAll contém o sellOld primeiro
	    verify(orderRepository).saveAll(argThat(iter -> {
	        List<Order> list = new ArrayList<>();
	        iter.forEach(list::add);

	        if (list.size() != 2) return false;

	        Order o1 = list.get(0);
	        Order o2 = list.get(1);

	        boolean hasOldExecuted = (o1.getId() == 2L && o1.getExecutedVolume() == 10.0 && o1.getStatus() == OrderStatus.EXECUTED)
	                               || (o2.getId() == 2L && o2.getExecutedVolume() == 10.0 && o2.getStatus() == OrderStatus.EXECUTED);

	        boolean hasRecentUnchanged = (o1.getId() == 1L && o1.getExecutedVolume() == 0.0 && o1.getStatus() == OrderStatus.PENDING)
	                                   || (o2.getId() == 1L && o2.getExecutedVolume() == 0.0 && o2.getStatus() == OrderStatus.PENDING);

	        return hasOldExecuted && hasRecentUnchanged;
	    }));



	    // ✅ Verifica que a ordem de compra também foi salva com status atualizado
	    verify(orderRepository).save(argThat(order ->
	        order.getId().equals(10L) &&
	        order.getStatus() == OrderStatus.EXECUTED &&
	        order.getExecutedVolume() == 10.0
	    ));
	}






}
