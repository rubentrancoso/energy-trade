package com.energytrade.logcollector;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

import com.energytrade.logcollector.model.LogEntry;

@SpringBootTest(classes = LogCollectorServiceApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public class LogCollectorControllerTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    public void shouldAcceptLogEntry() {
        LogEntry log = new LogEntry(
            "2025-06-01T20:48:00Z", // timestamp
            "INFO",                 // level
            "order-service",        // source
            "Pedido criado com sucesso.", // message
            "TestService" // service
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<LogEntry> request = new HttpEntity<>(log, headers);

        ResponseEntity<Void> response = restTemplate.postForEntity("/log", request, Void.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }
}
