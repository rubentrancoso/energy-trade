package com.energytrade.orderservice.model;

import javax.persistence.*;
import java.time.OffsetDateTime;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "orders")
@Data
@NoArgsConstructor
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String type;
    private Double price;
    private Double volume;
    private OffsetDateTime timestamp = OffsetDateTime.now();
    private String status = "PENDING";
    private double marketPrice;
}
