package com.vicky.smartpay.wallet.service;

import com.vicky.smartpay.wallet.dto.PaymentEvent;
import com.vicky.smartpay.wallet.model.Wallet;
import com.vicky.smartpay.wallet.model.WalletTransaction;
import com.vicky.smartpay.wallet.repository.WalletRepository;
import com.vicky.smartpay.wallet.repository.WalletTransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
public class WalletService {

    private final WalletRepository walletRepository;
    private final WalletTransactionRepository transactionRepository;

    public WalletService(WalletRepository walletRepository,
                         WalletTransactionRepository transactionRepository) {
        this.walletRepository = walletRepository;
        this.transactionRepository = transactionRepository;
    }

    // Create wallet for new user with initial balance
    public Wallet createWallet(Long userId, BigDecimal initialBalance) {
        if (walletRepository.findByUserId(userId).isPresent()) {
            throw new RuntimeException("Wallet already exists for user: " + userId);
        }
        Wallet wallet = new Wallet();
        wallet.setUserId(userId);
        wallet.setBalance(initialBalance);
        return walletRepository.save(wallet);
    }

    public Wallet getWallet(Long userId) {
        return walletRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Wallet not found for user: " + userId));
    }

    // Called when Kafka receives payment.initiated event
    @Transactional
    public void processPayment(PaymentEvent event) {
        try {
            // Deduct from sender
            Wallet senderWallet = walletRepository.findByUserId(event.getSenderId())
                    .orElseThrow(() -> new RuntimeException("Sender wallet not found"));

            if (senderWallet.getBalance().compareTo(event.getAmount()) < 0) {
                throw new RuntimeException("Insufficient balance");
            }

            senderWallet.setBalance(senderWallet.getBalance().subtract(event.getAmount()));
            walletRepository.save(senderWallet);

            // Credit to receiver
            Wallet receiverWallet = walletRepository.findByUserId(event.getReceiverId())
                    .orElseThrow(() -> new RuntimeException("Receiver wallet not found"));

            receiverWallet.setBalance(receiverWallet.getBalance().add(event.getAmount()));
            walletRepository.save(receiverWallet);

            // Record transaction
            saveTransaction(senderWallet.getId(), event.getPaymentId(),
                    event.getAmount(), "DEBIT", "SUCCESS");
            saveTransaction(receiverWallet.getId(), event.getPaymentId(),
                    event.getAmount(), "CREDIT", "SUCCESS");

            System.out.println("Payment processed successfully: " + event.getPaymentId());

        } catch (Exception e) {
            System.err.println("Payment processing failed: " + e.getMessage());
        }
    }

    private void saveTransaction(Long walletId, String paymentId,
                                 BigDecimal amount, String type, String status) {
        WalletTransaction tx = new WalletTransaction();
        tx.setWalletId(walletId);
        tx.setPaymentId(paymentId);
        tx.setAmount(amount);
        tx.setType(type);
        tx.setStatus(status);
        transactionRepository.save(tx);
    }
}