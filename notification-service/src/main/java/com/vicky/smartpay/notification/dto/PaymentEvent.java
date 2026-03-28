package com.vicky.smartpay.notification.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class PaymentEvent {
    private String paymentId;
    private Long senderId;
    private Long receiverId;
    private BigDecimal amount;
    private String status;
}