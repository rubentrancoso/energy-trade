package com.energytrade.orderservice;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import com.energytrade.orderservice.model.AuditEvent;
import com.energytrade.orderservice.model.AuditFallback;
import com.energytrade.orderservice.repository.AuditFallbackRepository;

import java.util.Iterator;

@Slf4j
@Component
@RequiredArgsConstructor
public class RetryAuditScheduler {

    private final AuditFallbackRepository fallbackRepository;
    private final RestTemplate restTemplate;
    private final OrderConfig orderConfig; // usado para obter auditUrl dinamicamente

    @Scheduled(fixedDelay = 60000) // a cada 60s
    public void retryFailedAudits() {
        Iterator<AuditFallback> iterator = fallbackRepository.findAll().iterator();

        while (iterator.hasNext()) {
            AuditFallback fallback = iterator.next();

            AuditEvent event = new AuditEvent(
                    fallback.getSource(),
                    fallback.getType(),
                    fallback.getPayload()
            );

            try {
                restTemplate.postForObject(orderConfig.getAuditServiceUrl(), event, Void.class);
                fallbackRepository.deleteById(fallback.getId());
                log.info("Reenvio de auditoria bem-sucedido para evento ID {}", fallback.getId());
            } catch (Exception e) {
                log.warn("Falha ao reenviar evento de auditoria ID {}: {}", fallback.getId(), e.getMessage());
            }
        }
    }
}