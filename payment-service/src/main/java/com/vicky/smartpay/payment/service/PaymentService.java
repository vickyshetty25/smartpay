package com.vicky.smartpay.payment.service;

import com.vicky.smartpay.payment.dto.PaymentEvent;
import com.vicky.smartpay.payment.dto.PaymentRequest;
import com.vicky.smartpay.payment.kafka.PaymentEventProducer;
import com.vicky.smartpay.payment.model.Payment;
import com.vicky.smartpay.payment.repository.PaymentRepository;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import java.time.Duration;
import java.util.List;
import java.util.UUID;

@Service
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final PaymentEventProducer eventProducer;
    private final RedisTemplate<String, String> redisTemplate;

    public PaymentService(PaymentRepository paymentRepository,
                          PaymentEventProducer eventProducer,
                          RedisTemplate<String, String> redisTemplate) {
        this.paymentRepository = paymentRepository;
        this.eventProducer = eventProducer;
        this.redisTemplate = redisTemplate;
    }

    public Payment initiatePayment(PaymentRequest request, Long senderId) {
        String idempotencyKey = request.getIdempotencyKey();
        String redisKey = "idempotency:" + idempotencyKey;

        // Check Redis first — if key exists, return original payment
        // This prevents duplicate payments!
        String existingPaymentId = redisTemplate.opsForValue().get(redisKey);
        if (existingPaymentId != null) {
            return paymentRepository.findByPaymentId(existingPaymentId)
                    .orElseThrow(() -> new RuntimeException("Payment not found"));
        }

        // Check DB as backup
        return paymentRepository.findByIdempotencyKey(idempotencyKey)
                .orElseGet(() -> createNewPayment(request, senderId, redisKey));
    }

    private Payment createNewPayment(PaymentRequest request,
                                     Long senderId, String redisKey) {
        // Create payment record
        Payment payment = new Payment();
        payment.setPaymentId(UUID.randomUUID().toString());
        payment.setSenderId(senderId);
        payment.setReceiverId(request.getReceiverId());
        payment.setAmount(request.getAmount());
        payment.setStatus("INITIATED");
        payment.setDescription(request.getDescription());
        payment.setIdempotencyKey(request.getIdempotencyKey());
        payment = paymentRepository.save(payment);

        // Store in Redis with 24hr TTL — idempotency window
        redisTemplate.opsForValue().set(redisKey, payment.getPaymentId(),
                Duration.ofHours(24));

        // Publish to Kafka — Wallet Service will consume this
        PaymentEvent event = new PaymentEvent(
                payment.getPaymentId(),
                payment.getSenderId(),
                payment.getReceiverId(),
                payment.getAmount(),
                "INITIATED"
        );
        eventProducer.publishPaymentInitiated(event);

        return payment;
    }

    public List<Payment> getPaymentHistory(Long userId) {
        return paymentRepository.findBySenderId(userId);
    }

    public Payment getPayment(String paymentId) {
        return paymentRepository.findByPaymentId(paymentId)
                .orElseThrow(() -> new RuntimeException("Payment not found"));
    }
}