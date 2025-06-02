package com.energytrade.notificationservice;

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

import com.energytrade.notificationservice.model.Notification;

@SpringBootTest(classes = NotificationServiceApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class NotificationControllerTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    public void shouldSendNotification() {
        Notification notification = new Notification("admin@energy.com", "Test message");
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Notification> request = new HttpEntity<>(notification, headers);
        ResponseEntity<Void> response = restTemplate.postForEntity("/notify", request, Void.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }
}
