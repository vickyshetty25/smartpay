package com.vicky.smartpay.notification.service;

import com.vicky.smartpay.notification.dto.PaymentEvent;
import com.vicky.smartpay.notification.model.Notification;
import com.vicky.smartpay.notification.repository.NotificationRepository;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;

    public NotificationService(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    public void handlePaymentCompleted(PaymentEvent event) {
        // Notify sender
        saveNotification(
                event.getSenderId(),
                "Your payment of ₹" + event.getAmount() +
                        " was sent successfully. Payment ID: " + event.getPaymentId(),
                event.getPaymentId(),
                "PAYMENT_SENT"
        );

        // Notify receiver
        saveNotification(
                event.getReceiverId(),
                "You received ₹" + event.getAmount() +
                        ". Payment ID: " + event.getPaymentId(),
                event.getPaymentId(),
                "PAYMENT_RECEIVED"
        );

        System.out.println("Notifications sent for payment: " + event.getPaymentId());
    }

    public void handlePaymentFailed(PaymentEvent event) {
        saveNotification(
                event.getSenderId(),
                "Your payment of ₹" + event.getAmount() +
                        " failed. Payment ID: " + event.getPaymentId(),
                event.getPaymentId(),
                "PAYMENT_FAILED"
        );

        System.out.println("Failure notification sent for payment: " + event.getPaymentId());
    }

    private void saveNotification(Long userId, String message,
                                  String paymentId, String type) {
        Notification notification = new Notification();
        notification.setUserId(userId);
        notification.setMessage(message);
        notification.setPaymentId(paymentId);
        notification.setType(type);
        notificationRepository.save(notification);

        // In real system: send SMS/email/push here
        System.out.println("NOTIFICATION [" + type + "] → User " +
                userId + ": " + message);
    }

    public List<Notification> getNotifications(Long userId) {
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }
}