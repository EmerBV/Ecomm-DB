package com.emerbv.ecommdb.service.payment;

import com.emerbv.ecommdb.request.PaymentRequest;
import com.emerbv.ecommdb.response.PaymentIntentResponse;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;

public interface IPaymentService {
    PaymentIntentResponse createPaymentIntent(PaymentRequest paymentRequest) throws StripeException;
    PaymentIntent confirmPayment(String paymentIntentId) throws StripeException;
    PaymentIntent cancelPayment(String paymentIntentId) throws StripeException;
    PaymentIntent retrievePayment(String paymentIntentId) throws StripeException;
}
