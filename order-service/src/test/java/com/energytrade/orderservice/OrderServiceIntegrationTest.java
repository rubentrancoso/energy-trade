package com.energytrade.orderservice;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import com.energytrade.orderservice.model.Order;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Import(AppConfig.class)
public class OrderServiceIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private RestTemplate rawRestTemplate;

    @Value("${pricing.service.url}")
    private String pricingUrl;

    @Value("${audit.service.url}")
    private String auditUrl;
    
    @Value("${notification.service.url}")
    private String notifyUrl;

    private MockRestServiceServer mockServer;

    @BeforeEach
    void setUp() {
        mockServer = MockRestServiceServer.bindTo(rawRestTemplate).ignoreExpectOrder(true).build();
    }

    @Test
    public void shouldCreateAndRetrieveOrders() {
        // mock de pricing
        String mockPricingResponse = "{\"value\":123.45,\"unit\":\"EUR/MWh\"}";
        mockServer.expect(once(), requestTo(pricingUrl))
                  .andRespond(withSuccess(mockPricingResponse, MediaType.APPLICATION_JSON));

        // mock de audit
        mockServer.expect(once(), requestTo(auditUrl))
                  .andRespond(withSuccess());
        
        // mock de notify
        mockServer.expect(once(), requestTo(notifyUrl))
                  .andRespond(withSuccess());

        // criação da ordem
        Order order = new Order();
        order.setType("BUY");
        order.setPrice(100.0);
        order.setVolume(10.0);

        ResponseEntity<Order> postResponse = restTemplate.postForEntity("/orders", order, Order.class);
        assertThat(postResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(postResponse.getBody()).isNotNull();
        assertThat(postResponse.getBody().getId()).isNotNull();
        assertThat(postResponse.getBody().getMarketPrice()).isEqualTo(123.45);

        // consulta
        ResponseEntity<Order[]> getResponse = restTemplate.getForEntity("/orders", Order[].class);
        assertThat(getResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(getResponse.getBody()).isNotEmpty();
    }
}
