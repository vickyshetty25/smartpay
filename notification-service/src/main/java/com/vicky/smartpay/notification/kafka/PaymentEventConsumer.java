package com.vicky.smartpay.notification.kafka;

import com.vicky.smartpay.notification.dto.PaymentEvent;
import com.vicky.smartpay.notification.service.NotificationService;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class PaymentEventConsumer {

    private final NotificationService notificationService;

    public PaymentEventConsumer(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @KafkaListener(topics = "payment.initiated", groupId = "notification-group")
    public void handlePaymentInitiated(PaymentEvent event) {
        System.out.println("Notification Service received payment.initiated: "
                + event.getPaymentId());
        // Treat initiated as completed for now
        notificationService.handlePaymentCompleted(event);
    }

    @KafkaListener(topics = "payment.failed", groupId = "notification-group")
    public void handlePaymentFailed(PaymentEvent event) {
        System.out.println("Notification Service received payment.failed: "
                + event.getPaymentId());
        notificationService.handlePaymentFailed(event);
    }
}