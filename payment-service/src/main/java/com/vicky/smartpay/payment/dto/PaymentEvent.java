package com.vicky.smartpay.payment.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

// This is published to Kafka — Wallet Service consumes it
@Data
@AllArgsConstructor
@NoArgsConstructor
public class PaymentEvent {
    private String paymentId;
    private Long senderId;
    private Long receiverId;
    private BigDecimal amount;
    private String status;
}