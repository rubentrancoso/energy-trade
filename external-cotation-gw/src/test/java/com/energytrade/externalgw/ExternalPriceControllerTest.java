package com.energytrade.externalgw;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.energytrade.externalgwservice.ExternalCotationGwApplication;
import com.energytrade.externalgwservice.ExternalPriceResponse;

@SpringBootTest(classes = ExternalCotationGwApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class ExternalPriceControllerTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    public void shouldReturnExternalPrice() {
        ResponseEntity<ExternalPriceResponse> response = restTemplate.getForEntity("/external-price", ExternalPriceResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getValue()).isGreaterThan(0.0);
        assertThat(response.getBody().getUnit()).isEqualTo("USD/MWh");
    }
}
