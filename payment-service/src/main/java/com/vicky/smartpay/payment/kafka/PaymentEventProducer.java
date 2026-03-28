package com.vicky.smartpay.payment.kafka;

import com.vicky.smartpay.payment.dto.PaymentEvent;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class PaymentEventProducer {

    private final KafkaTemplate<String, PaymentEvent> kafkaTemplate;

    public PaymentEventProducer(KafkaTemplate<String, PaymentEvent> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publishPaymentInitiated(PaymentEvent event) {
        kafkaTemplate.send("payment.initiated", event.getPaymentId(), event);
        System.out.println("Published payment.initiated event: " + event.getPaymentId());
    }

    public void publishPaymentCompleted(PaymentEvent event) {
        kafkaTemplate.send("payment.completed", event.getPaymentId(), event);
        System.out.println("Published payment.completed event: " + event.getPaymentId());
    }

    public void publishPaymentFailed(PaymentEvent event) {
        kafkaTemplate.send("payment.failed", event.getPaymentId(), event);
        System.out.println("Published payment.failed event: " + event.getPaymentId());
    }
}