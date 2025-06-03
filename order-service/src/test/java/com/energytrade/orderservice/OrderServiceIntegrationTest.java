package com.energytrade.orderservice;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.energytrade.orderservice.model.Order;
import com.energytrade.orderservice.model.OrderType;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.client.ExpectedCount;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@TestPropertySource(properties = {
    "spring.scheduling.enabled=false"
})
public class OrderServiceIntegrationTest {

    @Autowired
    private TestRestTemplate testRestTemplate;

    @Autowired
    private RestTemplateBuilder restTemplateBuilder;

    private RestTemplate rawRestTemplate;
    private MockRestServiceServer mockServer;

    @Value("${pricing.service.url}")
    private String pricingUrl;

    @Value("${audit.service.url}")
    private String auditUrl;

    @Value("${notification.service.url}")
    private String notifyUrl;

    @BeforeEach
    public void setup() {
        this.rawRestTemplate = restTemplateBuilder.build();
        this.mockServer = MockRestServiceServer.createServer(rawRestTemplate);
    }

    @Test
    public void shouldExecuteBuyOrderAndMatchWithSell() {
        // Mocks comuns
        mockServer.expect(ExpectedCount.manyTimes(), requestTo(pricingUrl))
                  .andRespond(withSuccess("{\"value\": 100.0, \"unit\": \"EUR/MWh\"}", MediaType.APPLICATION_JSON));

        mockServer.expect(ExpectedCount.manyTimes(), requestTo(auditUrl))
                  .andExpect(method(HttpMethod.POST))
                  .andRespond(withSuccess());

        mockServer.expect(ExpectedCount.manyTimes(), requestTo(notifyUrl))
                  .andExpect(method(HttpMethod.POST))
                  .andRespond(withSuccess());

        // Envia ordem SELL
        Order sellOrder = new Order();
        sellOrder.setType(OrderType.SELL);
        sellOrder.setPrice(100.0);
        sellOrder.setVolume(5.0);
        sellOrder.setMarketPrice(0.0);
        testRestTemplate.postForEntity("/orders", sellOrder, Order.class);

        // Envia ordem BUY para casar
        Order buyOrder = new Order();
        buyOrder.setType(OrderType.BUY);
        buyOrder.setPrice(110.0);
        buyOrder.setVolume(5.0);
        buyOrder.setMarketPrice(0.0);

        ResponseEntity<Order> response = testRestTemplate.postForEntity("/orders", buyOrder, Order.class);
        Order matchedOrder = response.getBody();

        assertThat(matchedOrder).isNotNull();
        assertThat(matchedOrder.getStatus().name()).isEqualTo("EXECUTED");
        assertThat(matchedOrder.getExecutedVolume()).isEqualTo(5.0);

        mockServer.verify();
    }
}


//@Test
//public void shouldNotRematchExecutedOrder() {
//    // Primeiro mock: SELL
//    MockRestServiceServer server1 = MockRestServiceServer.bindTo(rawRestTemplate).ignoreExpectOrder(true).build();
//    
//    server1.expect(ExpectedCount.manyTimes(), requestTo(pricingUrl)).andRespond(withSuccess("{\"value\":123.45}", MediaType.APPLICATION_JSON));
//    server1.expect(ExpectedCount.manyTimes(), requestTo(auditUrl)).andExpect(method(HttpMethod.POST)).andRespond(withSuccess());
//    server1.expect(ExpectedCount.manyTimes(), requestTo(notifyUrl)).andExpect(method(HttpMethod.POST)).andRespond(withSuccess());
//    server1.expect(anything()).andRespond(withSuccess());
//
//    Order sellOrder = new Order();
//    sellOrder.setType(OrderType.SELL);
//    sellOrder.setPrice(100.0);
//    sellOrder.setVolume(5.0);
//    sellOrder.setMarketPrice(0.0);
//    restTemplate.postForEntity("/orders", sellOrder, Order.class);
//
//    // Segundo mock: BUY
//    MockRestServiceServer server2 = MockRestServiceServer.bindTo(rawRestTemplate).ignoreExpectOrder(true).build();
//    server2.expect(requestTo(pricingUrl)).andRespond(withSuccess("{\"value\":100.0,\"unit\":\"EUR/MWh\"}", MediaType.APPLICATION_JSON));
//    server2.expect(requestTo(auditUrl)).andRespond(withSuccess());
//    server2.expect(requestTo(notifyUrl)).andRespond(withSuccess());
//
//    Order buyOrder = new Order();
//    buyOrder.setType(OrderType.BUY);
//    buyOrder.setPrice(110.0);
//    buyOrder.setVolume(5.0);
//    buyOrder.setMarketPrice(0.0);
//    ResponseEntity<Order> response = restTemplate.postForEntity("/orders", buyOrder, Order.class);
//
//    Order matchedOrder = response.getBody();
//    assertThat(matchedOrder).isNotNull();
//    assertThat(matchedOrder.getStatus().name()).isEqualTo("EXECUTED");
//    assertThat(matchedOrder.getExecutedVolume()).isEqualTo(5.0);
//}

//    @Test
//    @DirtiesContext
//    public void shouldCreateAndRetrieveOrders() {
//        // Reinicia o mockServer para evitar conflitos com testes anteriores
//        mockServer = MockRestServiceServer.bindTo(rawRestTemplate).ignoreExpectOrder(true).build();
//
//        String mockPricingResponse = "{\"value\":123.45,\"unit\":\"EUR/MWh\"}";
//        mockServer.expect(once(), requestTo(pricingUrl)).andRespond(withSuccess(mockPricingResponse, MediaType.APPLICATION_JSON));
//        mockServer.expect(once(), requestTo(auditUrl)).andRespond(withSuccess());
//        mockServer.expect(once(), requestTo(notifyUrl)).andRespond(withSuccess());
//
//        Order order = new Order();
//        order.setType(OrderType.BUY);
//        order.setPrice(100.0);
//        order.setVolume(10.0);
//        order.setMarketPrice(0.0);
//
//        ResponseEntity<Order> postResponse = restTemplate.postForEntity("/orders", order, Order.class);
//        assertThat(postResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
//        assertThat(postResponse.getBody()).isNotNull();
//        assertThat(postResponse.getBody().getId()).isNotNull();
//        assertThat(postResponse.getBody().getMarketPrice()).isEqualTo(123.45);
//
//        ResponseEntity<Order[]> getResponse = restTemplate.getForEntity("/orders", Order[].class);
//        assertThat(getResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
//        assertThat(getResponse.getBody()).isNotEmpty();
//    }
//
//
//    @Test
//    @DirtiesContext
//    public void shouldMatchBuyAndSellOrdersFully() {
//        // Reinicializa o mockServer para garantir isolamento do teste
//        mockServer = MockRestServiceServer.bindTo(rawRestTemplate).ignoreExpectOrder(true).build();
//
//        Order sellOrder = new Order();
//        sellOrder.setType(OrderType.SELL);
//        sellOrder.setPrice(100.0);
//        sellOrder.setVolume(5.0);
//        sellOrder.setMarketPrice(0.0);
//        mockServer.expect(requestTo(pricingUrl)).andRespond(withSuccess("{\"value\":100.0,\"unit\":\"EUR/MWh\"}", MediaType.APPLICATION_JSON));
//        mockServer.expect(requestTo(auditUrl)).andRespond(withSuccess());
//        mockServer.expect(requestTo(notifyUrl)).andRespond(withSuccess());
//        
//        Order buyOrder = new Order();
//        buyOrder.setType(OrderType.BUY);
//        buyOrder.setPrice(110.0);
//        buyOrder.setVolume(5.0);
//        buyOrder.setMarketPrice(0.0);
//        mockServer.expect(requestTo(pricingUrl)).andRespond(withSuccess("{\"value\":100.0,\"unit\":\"EUR/MWh\"}", MediaType.APPLICATION_JSON));
//        mockServer.expect(requestTo(auditUrl)).andRespond(withSuccess());
//        mockServer.expect(requestTo(notifyUrl)).andRespond(withSuccess());
//
//        
//        restTemplate.postForEntity("/orders", sellOrder, Order.class);
//        ResponseEntity<Order> response = restTemplate.postForEntity("/orders", buyOrder, Order.class);
//
//        Order matchedOrder = response.getBody();
//        assertThat(matchedOrder).isNotNull();
//        assertThat(matchedOrder.getStatus().name()).isEqualTo("EXECUTED");
//        assertThat(matchedOrder.getExecutedVolume()).isEqualTo(5.0);
//    }
//
//    @Test
//    @DirtiesContext
//    public void shouldPartiallyMatchWhenVolumeDiffers() {
//        Order sellOrder = new Order();
//        sellOrder.setType(OrderType.SELL);
//        sellOrder.setPrice(100.0);
//        sellOrder.setVolume(3.0);
//        sellOrder.setMarketPrice(0.0);
//        mockServer.expect(requestTo(pricingUrl)).andRespond(withSuccess("{\"value\":100.0,\"unit\":\"EUR/MWh\"}", MediaType.APPLICATION_JSON));
//        mockServer.expect(requestTo(auditUrl)).andRespond(withSuccess());
//        mockServer.expect(requestTo(notifyUrl)).andRespond(withSuccess());
//        
//        Order buyOrder = new Order();
//        buyOrder.setType(OrderType.BUY);
//        buyOrder.setPrice(110.0);
//        buyOrder.setVolume(5.0);
//        buyOrder.setMarketPrice(0.0);
//        mockServer.expect(requestTo(pricingUrl)).andRespond(withSuccess("{\"value\":100.0,\"unit\":\"EUR/MWh\"}", MediaType.APPLICATION_JSON));
//        mockServer.expect(requestTo(auditUrl)).andRespond(withSuccess());
//        mockServer.expect(requestTo(notifyUrl)).andRespond(withSuccess());
//
//        restTemplate.postForEntity("/orders", sellOrder, Order.class);
//        ResponseEntity<Order> response = restTemplate.postForEntity("/orders", buyOrder, Order.class);
//
//        Order matchedOrder = response.getBody();
//        assertThat(matchedOrder).isNotNull();
//        assertThat(matchedOrder.getStatus().name()).isEqualTo("PARTIAL");
//        assertThat(matchedOrder.getExecutedVolume()).isEqualTo(3.0);
//    }
//
//    @Test
//    @DirtiesContext
//    public void shouldNotRematchExecutedOrder() {
//        // SELL order
//        mockServer.expect(requestTo(pricingUrl)).andRespond(withSuccess("{\"value\":100.0,\"unit\":\"EUR/MWh\"}", MediaType.APPLICATION_JSON));
//        mockServer.expect(requestTo(auditUrl)).andRespond(withSuccess());
//        mockServer.expect(requestTo(notifyUrl)).andRespond(withSuccess());
//
//        Order sellOrder = new Order();
//        sellOrder.setType(OrderType.SELL);
//        sellOrder.setPrice(100.0);
//        sellOrder.setVolume(5.0);
//        sellOrder.setMarketPrice(0.0);
//        
//        // BUY order que irá consumir totalmente a sellOrder
//        mockServer.expect(requestTo(pricingUrl)).andRespond(withSuccess("{\"value\":100.0,\"unit\":\"EUR/MWh\"}", MediaType.APPLICATION_JSON));
//        mockServer.expect(requestTo(auditUrl)).andRespond(withSuccess());
//        mockServer.expect(requestTo(notifyUrl)).andRespond(withSuccess());
//
//        Order buyOrder = new Order();
//        buyOrder.setType(OrderType.BUY);
//        buyOrder.setPrice(110.0);
//        buyOrder.setVolume(5.0);
//        buyOrder.setMarketPrice(0.0);
//
//        restTemplate.postForEntity("/orders", sellOrder, Order.class);
//        ResponseEntity<Order> response = restTemplate.postForEntity("/orders", buyOrder, Order.class);
//
//        Order matchedOrder = response.getBody();
//        assertThat(matchedOrder).isNotNull();
//        assertThat(matchedOrder.getStatus().name()).isEqualTo("EXECUTED");
//        assertThat(matchedOrder.getExecutedVolume()).isEqualTo(5.0);
//    }
//
//
//
//    @Test
//    @DirtiesContext
//    public void shouldNotMatchWithIncompatiblePrices() {
//        mockServer.expect(once(), requestTo(pricingUrl)).andRespond(withSuccess("{\"value\":200.0,\"unit\":\"EUR/MWh\"}", MediaType.APPLICATION_JSON));
//        mockServer.expect(once(), requestTo(auditUrl)).andRespond(withSuccess());
//        mockServer.expect(once(), requestTo(notifyUrl)).andRespond(withSuccess());
//
//        Order sellOrder = new Order();
//        sellOrder.setType(OrderType.SELL);
//        sellOrder.setPrice(200.0);
//        sellOrder.setVolume(5.0);
//        sellOrder.setMarketPrice(0.0);
//        
//        mockServer = MockRestServiceServer.bindTo(rawRestTemplate).ignoreExpectOrder(true).build();
//
//        mockServer.expect(once(), requestTo(pricingUrl)).andRespond(withSuccess("{\"value\":150.0,\"unit\":\"EUR/MWh\"}", MediaType.APPLICATION_JSON));
//        mockServer.expect(once(), requestTo(auditUrl)).andRespond(withSuccess());
//        mockServer.expect(once(), requestTo(notifyUrl)).andRespond(withSuccess());
//
//        Order buyOrder = new Order();
//        buyOrder.setType(OrderType.BUY);
//        buyOrder.setPrice(150.0);
//        buyOrder.setVolume(5.0);
//        buyOrder.setMarketPrice(0.0);
//
//        restTemplate.postForEntity("/orders", sellOrder, Order.class);
//        ResponseEntity<Order> response = restTemplate.postForEntity("/orders", buyOrder, Order.class);
//
//        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
//        assertThat(response.getBody()).isNotNull();
//        Order unmatchedOrder = response.getBody();
//        assertThat(unmatchedOrder.getStatus().name()).isEqualTo("PENDING");
//        assertThat(unmatchedOrder.getExecutedVolume()).isEqualTo(0.0);
//    }


