package com.energytrade.orderservice;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.stream.Collectors;

import javax.persistence.EntityManager;
import javax.persistence.Query;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import com.energytrade.orderservice.model.Order;
import com.energytrade.orderservice.model.OrderStatus;
import com.energytrade.orderservice.model.OrderType;
import com.energytrade.orderservice.repository.OrderRepository;

@DataJpaTest
public class OrderStatusPersistenceTest {

	@Autowired
	private OrderRepository repository;

	@Autowired
	private EntityManager entityManager;

	@Test
	public void shouldPersistOrderStatusAsString() {
		for (OrderStatus status : OrderStatus.values()) {
			Order order = Order.builder().type(OrderType.BUY).price(100.0).volume(10.0).status(status) // aqui estava o
																										// erro
					.marketPrice(98.0).timestamp(OffsetDateTime.now())
					.expirationTimestamp(OffsetDateTime.now().plusHours(1)).build();

			repository.save(order);
		}

		entityManager.flush();
		entityManager.clear();

		Query query = entityManager.createNativeQuery("SELECT status FROM orders");
		@SuppressWarnings("unchecked")
		List<Object> rawResults = query.getResultList();

		List<String> statuses = rawResults.stream().map(Object::toString).collect(Collectors.toList());

		assertThat(statuses)
				.containsExactlyInAnyOrderElementsOf(List.of("PENDING", "PARTIAL", "EXECUTED", "CANCELLED", "EXPIRED"));

	}
}
