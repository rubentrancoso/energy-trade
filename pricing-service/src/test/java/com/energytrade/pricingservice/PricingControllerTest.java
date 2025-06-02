package com.energytrade.pricingservice;

import com.energytrade.pricingservice.model.PriceResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

@SpringBootTest
public class PricingControllerTest {

    @Value("${external.price.url}")
    private String externalPriceUrl;

    @Autowired
    private RestTemplateBuilder restTemplateBuilder;

    private RestTemplate restTemplate;
    private MockRestServiceServer mockServer;

    @BeforeEach
    public void setUp() {
        this.restTemplate = restTemplateBuilder.build();
        this.mockServer = MockRestServiceServer.createServer(restTemplate);
    }

    @Test
    public void shouldReturnValidPriceFromMockedExternalService() {
        String mockJson = "{\"value\": 112.5, \"unit\": \"EUR/MWh\"}";

        mockServer.expect(requestTo(externalPriceUrl))
                  .andRespond(withSuccess(mockJson, MediaType.APPLICATION_JSON));

        PriceResponse response = restTemplate.getForObject(externalPriceUrl, PriceResponse.class);

        assertThat(response).isNotNull();
        assertThat(response.getValue()).isEqualTo(112.5);
        assertThat(response.getUnit()).isEqualTo("EUR/MWh");

        mockServer.verify(); // Verifica se a URL foi realmente chamada
    }
}
