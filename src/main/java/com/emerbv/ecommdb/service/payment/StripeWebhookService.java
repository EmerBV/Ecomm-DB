package com.emerbv.ecommdb.service.payment;

import com.emerbv.ecommdb.enums.OrderStatus;
import com.emerbv.ecommdb.model.Order;
import com.emerbv.ecommdb.model.PaymentTransaction;
import com.emerbv.ecommdb.repository.OrderRepository;
import com.emerbv.ecommdb.repository.PaymentTransactionRepository;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.model.EventDataObjectDeserializer;
import com.stripe.model.PaymentIntent;
import com.stripe.model.StripeObject;
import com.stripe.net.Webhook;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class StripeWebhookService {
    private static final Logger logger = LoggerFactory.getLogger(StripeWebhookService.class);

    private final OrderRepository orderRepository;
    private final PaymentTransactionRepository transactionRepository;

    @Value("${stripe.webhook.secret}")
    private String endpointSecret;

    @Transactional
    public void processWebhookEvent(String payload, String signatureHeader) {
        if (endpointSecret == null || endpointSecret.isEmpty()) {
            logger.warn("Stripe webhook secret not configured");
            return;
        }

        Event event;
        try {
            event = Webhook.constructEvent(payload, signatureHeader, endpointSecret);
        } catch (SignatureVerificationException e) {
            logger.error("Invalid signature", e);
            return;
        }

        // Deserialize the event object to get the specific event type
        EventDataObjectDeserializer dataObjectDeserializer = event.getDataObjectDeserializer();
        StripeObject stripeObject = null;
        if (dataObjectDeserializer.getObject().isPresent()) {
            stripeObject = dataObjectDeserializer.getObject().get();
        } else {
            logger.error("Failed to deserialize stripe object from event");
            return;
        }

        // Handle different event types
        switch (event.getType()) {
            case "payment_intent.succeeded":
                handlePaymentIntentSucceeded((PaymentIntent) stripeObject);
                break;
            case "payment_intent.payment_failed":
                handlePaymentIntentFailed((PaymentIntent) stripeObject);
                break;
            case "payment_intent.canceled":
                handlePaymentIntentCanceled((PaymentIntent) stripeObject);
                break;
            default:
                logger.info("Unhandled event type: {}", event.getType());
        }
    }

    @Transactional
    private void handlePaymentIntentSucceeded(PaymentIntent paymentIntent) {
        logger.info("Payment succeeded: {}", paymentIntent.getId());
        String orderId = paymentIntent.getMetadata().get("orderId");
        if (orderId != null) {
            orderRepository.findById(Long.valueOf(orderId)).ifPresent(order -> {
                // Actualizar estado de la orden
                order.setOrderStatus(OrderStatus.PAID);

                // Actualizar información de pago en la orden
                order.setPaymentMethod(paymentIntent.getPaymentMethod());
                order.setPaymentIntentId(paymentIntent.getId());

                orderRepository.save(order);
                logger.info("Order {} updated to PAID with payment method {} and intent {}",
                        orderId, paymentIntent.getPaymentMethod(), paymentIntent.getId());

                // Actualizar o crear la transacción de pago
                updatePaymentTransaction(paymentIntent, order, "succeeded", null);
            });
        }
    }

    @Transactional
    private void handlePaymentIntentFailed(PaymentIntent paymentIntent) {
        logger.info("Payment failed: {}", paymentIntent.getId());
        String orderId = paymentIntent.getMetadata().get("orderId");
        if (orderId != null) {
            orderRepository.findById(Long.valueOf(orderId)).ifPresent(order -> {
                // No cambiamos el estado a cancelado automáticamente por si el usuario quiere reintentar
                logger.info("Payment failed for order {}", orderId);

                // Actualizar o crear la transacción de pago
                updatePaymentTransaction(paymentIntent, order, "failed",
                        paymentIntent.getLastPaymentError() != null ?
                                paymentIntent.getLastPaymentError().getMessage() : "Payment processing failed");
            });
        }
    }

    @Transactional
    private void handlePaymentIntentCanceled(PaymentIntent paymentIntent) {
        logger.info("Payment canceled: {}", paymentIntent.getId());
        String orderId = paymentIntent.getMetadata().get("orderId");
        if (orderId != null) {
            orderRepository.findById(Long.valueOf(orderId)).ifPresent(order -> {
                order.setOrderStatus(OrderStatus.CANCELLED);
                orderRepository.save(order);
                logger.info("Order {} updated to CANCELLED", orderId);

                // Actualizar o crear la transacción de pago
                updatePaymentTransaction(paymentIntent, order, "canceled", "Payment was canceled");
            });
        }
    }

    private void updatePaymentTransaction(PaymentIntent paymentIntent, Order order, String status, String errorMessage) {
        transactionRepository.findByPaymentIntentId(paymentIntent.getId())
                .ifPresentOrElse(
                        transaction -> {
                            transaction.setStatus(status);
                            transaction.setErrorMessage(errorMessage);
                            transaction.setPaymentMethod(paymentIntent.getPaymentMethod());
                            transactionRepository.save(transaction);
                        },
                        () -> {
                            // Si no existe la transacción, crearla
                            PaymentTransaction newTransaction = new PaymentTransaction(
                                    paymentIntent.getId(),
                                    order.getTotalAmount(),
                                    paymentIntent.getCurrency(),
                                    status,
                                    paymentIntent.getPaymentMethod(),
                                    order
                            );
                            newTransaction.setErrorMessage(errorMessage);
                            transactionRepository.save(newTransaction);
                        }
                );
    }
}
