package com.vicky.smartpay.wallet.kafka;

import com.vicky.smartpay.wallet.dto.PaymentEvent;
import com.vicky.smartpay.wallet.service.WalletService;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class PaymentEventConsumer {

    private final WalletService walletService;

    public PaymentEventConsumer(WalletService walletService) {
        this.walletService = walletService;
    }

    // This method is called automatically when a message
    // arrives on the payment.initiated topic
    @KafkaListener(topics = "payment.initiated", groupId = "wallet-group")
    public void handlePaymentInitiated(PaymentEvent event) {
        System.out.println("Wallet Service received payment event: " + event.getPaymentId());
        walletService.processPayment(event);
    }
}