package com.energytrade.orderservice;

import java.time.OffsetDateTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.energytrade.orderservice.model.Order;
import com.energytrade.orderservice.model.OrderStatus;
import com.energytrade.orderservice.repository.OrderRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderCleanupScheduler {

    private final OrderRepository orderRepository;

    @Value("${order.cleanup.enabled:true}")
    private boolean cleanupEnabled;

    @Scheduled(cron = "${order.cleanup.cron:0 0 2 * * *}") // default: 2h da manhã
    public void expirePendingOrders() {
        if (!cleanupEnabled) {
            log.info("🧹 Cleanup desativado por configuração");
            return;
        }

        OffsetDateTime now = OffsetDateTime.now();
        List<Order> expired = orderRepository.findByStatusAndExpirationTimestampBefore(OrderStatus.PENDING, now);

        if (expired.isEmpty()) {
            log.info("🧹 Nenhuma ordem pendente expirada encontrada");
            return;
        }

        for (Order order : expired) {
            order.setStatus(OrderStatus.EXPIRED);
        }

        orderRepository.saveAll(expired);
        log.info("🧹 {} ordens expiradas foram marcadas como EXPIRED", expired.size());
    }
}
