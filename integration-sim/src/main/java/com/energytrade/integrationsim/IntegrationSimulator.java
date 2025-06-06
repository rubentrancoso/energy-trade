
package com.energytrade.integrationsim;

import java.time.OffsetDateTime;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import com.energytrade.orderservice.model.Order;
import com.energytrade.orderservice.model.OrderType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;

@SpringBootApplication
public class IntegrationSimulator implements CommandLineRunner {

	private static final Logger log = LogManager.getLogger(IntegrationSimulator.class);

	@Value("${order.service.url}")
	private String orderUrl;

	@Value("${pricing.service.url}")
	private String pricingUrl;

	private final RestTemplate restTemplate = new RestTemplate();
	private final ObjectWriter prettyPrinter = new ObjectMapper().writerWithDefaultPrettyPrinter();

	public static void main(String[] args) {
		SpringApplication.run(IntegrationSimulator.class, args);
	}

	@Override
	public void run(String... args) {
		try {
			log.info("🔁 Requesting current market price...");
			ResponseEntity<String> priceResponse = restTemplate.getForEntity(pricingUrl, String.class);
			log.info("✔ Market price returned by pricing-service:\n{}",
					prettyPrinter.writeValueAsString(new ObjectMapper().readTree(priceResponse.getBody())));

			runStandardSimulations();
			runExpirationEdgeCaseTests();

			log.info("📥 Fetching final list of orders for verification...");
			ResponseEntity<String> allOrders = restTemplate.getForEntity(orderUrl, String.class);
			log.info("📄 Result of GET /orders:\n{}",
					prettyPrinter.writeValueAsString(new ObjectMapper().readTree(allOrders.getBody())));

			log.info("🎯 Simulation completed successfully.");
			log.info("🚀 Integration Simulator is up and running! 🌐");

		} catch (Exception e) {
			log.error("❌ Critical error during simulation: {}", e.getMessage(), e);
		}
	}

	private void runStandardSimulations() {
		log.info("🧪 Sending standard test orders...");
		List<Order> testOrders = List.of(
				Order.builder().type(OrderType.BUY).price(80.0).volume(10.0).marketPrice(75.0 + Math.random())
						.expirationTimestamp(OffsetDateTime.now().plusHours(1)).build(),

				Order.builder().type(OrderType.SELL).price(120.0).volume(5.0).marketPrice(115.0 + Math.random())
						.expirationTimestamp(OffsetDateTime.now().plusHours(1)).build(),

				Order.builder().type(OrderType.BUY).price(110.0).volume(7.0).marketPrice(108.0 + Math.random())
						.expirationTimestamp(OffsetDateTime.now().plusHours(1)).build(),

				Order.builder().type(OrderType.SELL).price(100.0).volume(3.0).marketPrice(99.0 + Math.random())
						.expirationTimestamp(OffsetDateTime.now().plusHours(1)).build(),

				Order.builder().type(OrderType.SELL).price(90.0).volume(5.0).marketPrice(89.0 + Math.random())
						.expirationTimestamp(OffsetDateTime.now().plusHours(1)).build(),

				Order.builder().type(OrderType.BUY).price(95.0).volume(5.0).marketPrice(94.0 + Math.random())
						.expirationTimestamp(OffsetDateTime.now().plusHours(1)).build(),

				Order.builder().type(OrderType.BUY).price(100.0).volume(0.0).marketPrice(98.0 + Math.random())
						.expirationTimestamp(OffsetDateTime.now().plusHours(1)).build(),

				Order.builder().type(OrderType.SELL).price(1000000.0).volume(1.0).marketPrice(999999.0 + Math.random())
						.expirationTimestamp(OffsetDateTime.now().plusHours(1)).build(),

				Order.builder().type(OrderType.BUY).price(0.01).volume(1.0).marketPrice(0.009 + Math.random())
						.expirationTimestamp(OffsetDateTime.now().plusHours(1)).build(),

				Order.builder().type(OrderType.SELL).price(100.0).volume(-5.0).marketPrice(99.0 + Math.random())
						.expirationTimestamp(OffsetDateTime.now().plusHours(1)).build(),

				Order.builder().type(OrderType.SELL).price(100.0).volume(5.0).marketPrice(99.0 + Math.random())
						.expirationTimestamp(OffsetDateTime.now().plusHours(1)).build(),

				Order.builder().type(OrderType.SELL).price(100.0).volume(5.0).marketPrice(99.0 + Math.random())
						.expirationTimestamp(OffsetDateTime.now().plusHours(1)).build(),

				Order.builder().type(OrderType.BUY).price(100.0).volume(5.0).marketPrice(98.0 + Math.random())
						.expirationTimestamp(OffsetDateTime.now().plusHours(1)).build(),

				Order.builder().type(OrderType.SELL).price(80.0).volume(10.0).marketPrice(79.0 + Math.random())
						.expirationTimestamp(OffsetDateTime.now().plusHours(1)).build(),

				Order.builder().type(OrderType.BUY).price(100.0).volume(9999999.0).marketPrice(98.0 + Math.random())
						.expirationTimestamp(OffsetDateTime.now().plusHours(1)).build(),

				Order.builder().type(OrderType.SELL).price(95.0).volume(9999999.0).marketPrice(94.0 + Math.random())
						.expirationTimestamp(OffsetDateTime.now().plusHours(1)).build(),

				Order.builder().type(OrderType.SELL).price(105.0).volume(5.0).marketPrice(104.0 + Math.random())
						.expirationTimestamp(OffsetDateTime.now().plusHours(1)).build(),

				Order.builder().type(OrderType.BUY).price(105.0).volume(5.0).marketPrice(104.0 + Math.random())
						.expirationTimestamp(OffsetDateTime.now().plusHours(1)).build(),

				Order.builder().type(OrderType.SELL).price(105.0).volume(5.0).marketPrice(104.0 + Math.random())
						.expirationTimestamp(OffsetDateTime.now().plusHours(1)).build());

		sendOrders(testOrders);
	}

	private void runExpirationEdgeCaseTests() throws InterruptedException {
		log.info("🕒 Running expiration edge case tests...");

		// Order already expired
		Order expired = Order.builder()
			    .type(OrderType.BUY)
			    .price(100.0)
			    .volume(10.0)
			    .marketPrice(98.0 + Math.random())
			    .expirationTimestamp(OffsetDateTime.now().minusMinutes(1))
			    .build();
		expired.setExpirationTimestamp(OffsetDateTime.now().minusMinutes(1));
		sendOrders(List.of(expired));

		// Valid order that expires in 5 seconds
		Order soonExpiring = Order.builder()
			    .type(OrderType.SELL)
			    .price(90.0)
			    .volume(5.0)
			    .marketPrice(88.0 + Math.random())
			    .expirationTimestamp(OffsetDateTime.now().plusSeconds(5))
			    .build();
		soonExpiring.setExpirationTimestamp(OffsetDateTime.now().plusSeconds(5));
		sendOrders(List.of(soonExpiring));
		log.info("⏳ Waiting for order to expire...");
		Thread.sleep(7000);
	}

	private void sendOrders(List<Order> orders) {
		int i = 1;
		for (Order order : orders) {
			log.info("📦 Sending order #{}: {}", i, order);
			try {
				ResponseEntity<Order> response = restTemplate.postForEntity(orderUrl, order, Order.class);
				if (response.getStatusCode().is2xxSuccessful()) {
					log.info("✅ Order successfully created: {}", response.getBody());
				} else {
					log.warn("⚠️ Order #{} failed: HTTP {}", i, response.getStatusCode());
				}
			} catch (Exception ex) {
				log.error("❌ Exception while sending order #{}: {}", i, ex.getMessage());
			}
			i++;
		}
	}
}
