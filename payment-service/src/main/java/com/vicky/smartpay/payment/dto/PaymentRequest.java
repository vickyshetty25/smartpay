package com.vicky.smartpay.payment.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class PaymentRequest {

    @NotNull
    private Long receiverId;

    @NotNull
    @Positive
    private BigDecimal amount;

    private String description;

    // Client sends this UUID to prevent duplicate payments
    @NotNull
    private String idempotencyKey;
}