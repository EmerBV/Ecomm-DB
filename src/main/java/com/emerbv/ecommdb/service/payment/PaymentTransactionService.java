package com.emerbv.ecommdb.service.payment;

import com.emerbv.ecommdb.exceptions.ResourceNotFoundException;
import com.emerbv.ecommdb.model.PaymentTransaction;
import com.emerbv.ecommdb.repository.PaymentTransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PaymentTransactionService implements IPaymentTransactionService {

    private final PaymentTransactionRepository transactionRepository;

    @Override
    public List<PaymentTransaction> getTransactionsByOrderId(Long orderId) {
        List<PaymentTransaction> transactions = transactionRepository.findByOrderOrderId(orderId);
        if (transactions.isEmpty()) {
            throw new ResourceNotFoundException("No transactions found for order ID: " + orderId);
        }
        return transactions;
    }

    @Override
    public PaymentTransaction getTransactionByPaymentIntentId(String paymentIntentId) {
        return transactionRepository.findByPaymentIntentId(paymentIntentId)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found with payment intent ID: " + paymentIntentId));
    }

    @Override
    public PaymentTransaction saveTransaction(PaymentTransaction transaction) {
        return transactionRepository.save(transaction);
    }
}
