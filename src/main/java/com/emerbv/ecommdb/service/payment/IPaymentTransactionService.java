package com.emerbv.ecommdb.service.payment;

import com.emerbv.ecommdb.model.PaymentTransaction;

import java.util.List;

public interface IPaymentTransactionService {
    List<PaymentTransaction> getTransactionsByOrderId(Long orderId);
    PaymentTransaction getTransactionByPaymentIntentId(String paymentIntentId);
    PaymentTransaction saveTransaction(PaymentTransaction transaction);
}
