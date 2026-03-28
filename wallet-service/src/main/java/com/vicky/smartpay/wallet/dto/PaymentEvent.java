package com.vicky.smartpay.wallet.dto;

import lombok.Data;
import java.math.BigDecimal;

// This is the event we receive from Payment Service via Kafka
@Data
public class PaymentEvent {
    private String paymentId;
    private Long senderId;
    private Long receiverId;
    private BigDecimal amount;
    private String status;
}