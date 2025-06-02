package com.energytrade.orderservice.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "audit_fallbacks")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuditFallback {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String source;
    private String type;

    @Lob
    private String payload;

    private LocalDateTime createdAt = LocalDateTime.now();
}