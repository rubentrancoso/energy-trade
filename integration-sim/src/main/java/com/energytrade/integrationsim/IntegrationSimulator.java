package com.energytrade.integrationsim;

import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

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
            log.info("✔ Market price returned by pricing-service:\n{}", prettyPrinter.writeValueAsString(
                    new ObjectMapper().readTree(priceResponse.getBody())
            ));

            log.info("🧪 Sending orders to trigger different use cases...");

            /**
             * This list simulates various real-world scenarios for order matching:
             *
             * 1. PENDING: Buy/Sell orders with no match due to price gap.
             * 2. PARTIAL: Buy/Sell orders that only match partially.
             * 3. FULL: Buy/Sell orders that match perfectly.
             * 4. EDGE CASES:
             *    - Volume = 0
             *    - Extremely high/low price
             *    - Negative volume
             *    - Duplicate orders
             */
            List<Order> testOrders = List.of(
                // --- Typical Orders ---
                new Order("BUY", 80.0, 10.0),          // No match, should remain PENDING
                new Order("SELL", 120.0, 5.0),         // No match, should remain PENDING
                new Order("BUY", 110.0, 7.0),          // Will partially match later
                new Order("SELL", 100.0, 3.0),         // Will match partially with previous
                new Order("SELL", 90.0, 5.0),          // Will match fully
                new Order("BUY", 95.0, 5.0),           // Will match fully

                // --- Edge Case Tests ---
                new Order("BUY", 100.0, 0.0),          // Zero volume — should be ignored or accepted as inert
                new Order("SELL", 1000000.0, 1.0),     // Extreme high price — should stay PENDING
                new Order("BUY", 0.01, 1.0),           // Extreme low price — should stay PENDING
                new Order("SELL", 100.0, -5.0),        // Negative volume — may throw error
                new Order("SELL", 100.0, 5.0),         // Duplicate #1
                new Order("SELL", 100.0, 5.0),          // Duplicate #2
                
                // --- Additional Edge Case Tests ---
                new Order("BUY", 100.0, 5.0),             // Matches one of the duplicate SELLs
                new Order("SELL", 80.0, 10.0),            // Should match with earlier BUY
                new Order("BUY", 100.0, 9999999.0),       // Extreme volume to test matching engine limits
                new Order("SELL", 95.0, 9999999.0),       // Another extreme volume order for stress
                new Order("SELL", 105.0, 5.0),            // Small order, possible match edge
                new Order("BUY", 105.0, 5.0),             // Counterpart to previous SELL, exact price match
                new Order("SELL", 105.0, 5.0)             // One more to test repeatability and volume handling

            );

            int i = 1;
            for (Order order : testOrders) {
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

            // Final retrieval for verification — all orders currently in the system
            log.info("📥 Fetching final list of orders for verification...");
            ResponseEntity<String> allOrders = restTemplate.getForEntity(orderUrl, String.class);

            log.info("📄 Result of GET /orders:\n{}", prettyPrinter.writeValueAsString(
                    new ObjectMapper().readTree(allOrders.getBody())
            ));

            log.info("🎯 Simulation completed successfully.");
            log.info("🚀 Integration Simulator is up and running! 🌐");

        } catch (Exception e) {
            log.error("❌ Critical error during simulation: {}", e.getMessage(), e);
        }
    }
}
