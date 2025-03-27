package com.emerbv.ecommdb.controller;

import com.emerbv.ecommdb.exceptions.ResourceNotFoundException;
import com.emerbv.ecommdb.model.PaymentTransaction;
import com.emerbv.ecommdb.response.ApiResponse;
import com.emerbv.ecommdb.service.payment.IPaymentTransactionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RequiredArgsConstructor
@RestController
@RequestMapping("${api.prefix}/payment-transactions")
public class PaymentTransactionController {

    private final IPaymentTransactionService transactionService;

    @GetMapping("/order/{orderId}")
    public ResponseEntity<ApiResponse> getTransactionsByOrderId(@PathVariable Long orderId) {
        try {
            List<PaymentTransaction> transactions = transactionService.getTransactionsByOrderId(orderId);
            return ResponseEntity.ok(new ApiResponse("Transactions retrieved successfully", transactions));
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ApiResponse(e.getMessage(), null));
        }
    }

    @GetMapping("/payment-intent/{paymentIntentId}")
    public ResponseEntity<ApiResponse> getTransactionByPaymentIntentId(@PathVariable String paymentIntentId) {
        try {
            PaymentTransaction transaction = transactionService.getTransactionByPaymentIntentId(paymentIntentId);
            return ResponseEntity.ok(new ApiResponse("Transaction retrieved successfully", transaction));
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ApiResponse(e.getMessage(), null));
        }
    }
}
