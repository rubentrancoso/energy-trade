package com.energytrade.integrationsim;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

@SpringBootApplication
public class IntegrationSimulator implements CommandLineRunner {

    @Value("${order.service.url}")
    private String orderUrl;

    @Value("${pricing.service.url}")
    private String pricingUrl;

    @Value("${audit.service.url}")
    private String auditUrl;

    private final RestTemplate restTemplate = new RestTemplate();

    public static void main(String[] args) {
        SpringApplication.run(IntegrationSimulator.class, args);
    }

    @Override
    public void run(String... args) {
        try {
            System.out.println("\n🔁 Consultando preço atual...");
            ResponseEntity<String> priceResponse = restTemplate.getForEntity(pricingUrl, String.class);
            System.out.println("✔ Preço retornado do pricing-service: " + priceResponse.getBody());

            List<Order> testOrders = List.of(
                    new Order("BUY", 100.0, 10.0),
                    new Order("SELL", 110.0, 5.0),
                    new Order("BUY", 120.5, 8.0),
                    new Order("SELL", 90.75, 12.0), 
                    new Order("BUY", 105.25, 7.5)
            );

            int i = 1;
            for (Order order : testOrders) {
                System.out.println("\n📦 Enviando ordem #" + i + ": " + order);
                ResponseEntity<Order> response = restTemplate.postForEntity(orderUrl, order, Order.class);

                if (response.getStatusCode().is2xxSuccessful()) {
                    System.out.println("✅ Ordem criada com sucesso: " + response.getBody());
                } else {
                    System.out.println("❌ Falha ao criar ordem #" + i + ": " + response.getStatusCode());
                }
                i++;
            }

            System.out.println("\n🎯 Simulação concluída com sucesso.");
        } catch (Exception e) {
            System.err.println("❌ Erro na simulação: " + e.getMessage());
            e.printStackTrace();
        }
    }

}
