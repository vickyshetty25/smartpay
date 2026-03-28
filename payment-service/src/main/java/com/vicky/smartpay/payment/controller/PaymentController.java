package com.vicky.smartpay.payment.controller;

import com.vicky.smartpay.payment.dto.PaymentRequest;
import com.vicky.smartpay.payment.model.Payment;
import com.vicky.smartpay.payment.security.JwtUtil;
import com.vicky.smartpay.payment.service.PaymentService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    private final PaymentService paymentService;
    private final JwtUtil jwtUtil;

    public PaymentController(PaymentService paymentService, JwtUtil jwtUtil) {
        this.paymentService = paymentService;
        this.jwtUtil = jwtUtil;
    }

    @PostMapping("/initiate")
    public ResponseEntity<Payment> initiatePayment(
            @Valid @RequestBody PaymentRequest request,
            @RequestHeader("Authorization") String authHeader) {

        String token = authHeader.replace("Bearer ", "");

        if (!jwtUtil.isTokenValid(token)) {
            return ResponseEntity.status(401).build();
        }

        Long senderId = jwtUtil.extractUserId(token);
        return ResponseEntity.ok(paymentService.initiatePayment(request, senderId));
    }

    @GetMapping("/history")
    public ResponseEntity<List<Payment>> getHistory(
            @RequestHeader("Authorization") String authHeader) {

        String token = authHeader.replace("Bearer ", "");
        Long userId = jwtUtil.extractUserId(token);
        return ResponseEntity.ok(paymentService.getPaymentHistory(userId));
    }

    @GetMapping("/{paymentId}")
    public ResponseEntity<Payment> getPayment(@PathVariable String paymentId) {
        return ResponseEntity.ok(paymentService.getPayment(paymentId));
    }
}