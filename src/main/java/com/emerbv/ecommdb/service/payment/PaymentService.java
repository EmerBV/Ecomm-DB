package com.emerbv.ecommdb.service.payment;

import com.emerbv.ecommdb.enums.OrderStatus;
import com.emerbv.ecommdb.exceptions.ResourceNotFoundException;
import com.emerbv.ecommdb.model.Order;
import com.emerbv.ecommdb.model.PaymentTransaction;
import com.emerbv.ecommdb.repository.OrderRepository;
import com.emerbv.ecommdb.repository.PaymentTransactionRepository;
import com.emerbv.ecommdb.request.PaymentRequest;
import com.emerbv.ecommdb.response.PaymentIntentResponse;
import com.emerbv.ecommdb.util.StripeUtils;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class PaymentService implements IPaymentService {

    private final OrderRepository orderRepository;
    private final PaymentTransactionRepository transactionRepository;
    private final StripeUtils stripeUtils;

    @Override
    @Transactional
    public PaymentIntentResponse createPaymentIntent(PaymentRequest paymentRequest) throws StripeException {
        // Obtener el orden que queremos pagar
        Order order = orderRepository.findById(paymentRequest.getOrderId())
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + paymentRequest.getOrderId()));

        // Convertir el monto al formato de Stripe (centavos/peniques)
        Long amount = stripeUtils.convertAmountToStripeFormat(order.getTotalAmount());

        // Crear los parámetros para Stripe PaymentIntent
        Map<String, Object> params = new HashMap<>();
        params.put("amount", amount);
        params.put("currency", paymentRequest.getCurrency() != null ? paymentRequest.getCurrency() : "usd");

        Map<String, String> metadata = new HashMap<>();
        metadata.put("orderId", order.getOrderId().toString());
        params.put("metadata", metadata);

        if (paymentRequest.getPaymentMethodId() != null) {
            params.put("payment_method", paymentRequest.getPaymentMethodId());
            params.put("confirm", true);
            params.put("confirmation_method", "manual");
        }

        if (paymentRequest.getReceiptEmail() != null) {
            params.put("receipt_email", paymentRequest.getReceiptEmail());
        }

        if (paymentRequest.getDescription() != null) {
            params.put("description", paymentRequest.getDescription());
        } else {
            params.put("description", "Payment for Order #" + order.getOrderId());
        }

        // Crear el PaymentIntent
        PaymentIntent intent = PaymentIntent.create(params);

        // Actualizar el estado de la orden
        order.setOrderStatus(OrderStatus.PROCESSING);
        orderRepository.save(order);

        // Guardar la transacción de pago
        PaymentTransaction transaction = new PaymentTransaction(
                intent.getId(),
                order.getTotalAmount(),
                paymentRequest.getCurrency() != null ? paymentRequest.getCurrency() : "usd",
                intent.getStatus(),
                paymentRequest.getPaymentMethodId(),
                order
        );
        transactionRepository.save(transaction);

        return new PaymentIntentResponse(intent.getClientSecret(), intent.getId());
    }

    @Override
    @Transactional
    public PaymentIntent confirmPayment(String paymentIntentId) throws StripeException {
        PaymentIntent intent = PaymentIntent.retrieve(paymentIntentId);
        Map<String, Object> params = new HashMap<>();
        PaymentIntent confirmedIntent = intent.confirm(params);

        // Si el pago fue exitoso, actualizar el estado de la orden
        if ("succeeded".equals(confirmedIntent.getStatus())) {
            String orderId = confirmedIntent.getMetadata().get("orderId");
            if (orderId != null) {
                orderRepository.findById(Long.valueOf(orderId)).ifPresent(order -> {
                    order.setOrderStatus(OrderStatus.PROCESSING);
                    orderRepository.save(order);

                    // Actualizar la transacción de pago
                    transactionRepository.findByPaymentIntentId(paymentIntentId).ifPresent(transaction -> {
                        transaction.setStatus(confirmedIntent.getStatus());
                        transaction.setPaymentMethod(confirmedIntent.getPaymentMethod());
                        transactionRepository.save(transaction);
                    });
                });
            }
        }

        return confirmedIntent;
    }

    @Override
    @Transactional
    public PaymentIntent cancelPayment(String paymentIntentId) throws StripeException {
        PaymentIntent intent = PaymentIntent.retrieve(paymentIntentId);
        PaymentIntent canceledIntent = intent.cancel();

        // Actualizar el estado de la orden a cancelado
        String orderId = canceledIntent.getMetadata().get("orderId");
        if (orderId != null) {
            orderRepository.findById(Long.valueOf(orderId)).ifPresent(order -> {
                order.setOrderStatus(OrderStatus.CANCELLED);
                orderRepository.save(order);

                // Actualizar la transacción de pago
                transactionRepository.findByPaymentIntentId(paymentIntentId).ifPresent(transaction -> {
                    transaction.setStatus("canceled");
                    transaction.setErrorMessage("Payment canceled by user or system");
                    transactionRepository.save(transaction);
                });
            });
        }

        return canceledIntent;
    }

    @Override
    @Transactional(readOnly = true)
    public PaymentIntent retrievePayment(String paymentIntentId) throws StripeException {
        PaymentIntent intent = PaymentIntent.retrieve(paymentIntentId);

        // Opcionalmente, actualizar la transacción local si hay cambios en el estado
        transactionRepository.findByPaymentIntentId(paymentIntentId).ifPresent(transaction -> {
            if (!transaction.getStatus().equals(intent.getStatus())) {
                transaction.setStatus(intent.getStatus());
                transactionRepository.save(transaction);
            }
        });

        return intent;
    }
}
