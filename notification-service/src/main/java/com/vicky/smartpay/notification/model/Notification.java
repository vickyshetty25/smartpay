package com.vicky.smartpay.notification.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "notifications")
@Data
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(nullable = false)
    private String message;

    @Column(name = "payment_id")
    private String paymentId;

    @Column(nullable = false)
    private String type; // PAYMENT_SENT, PAYMENT_RECEIVED, PAYMENT_FAILED

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();
}