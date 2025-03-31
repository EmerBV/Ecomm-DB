package com.emerbv.ecommdb.service.payment;

import com.emerbv.ecommdb.enums.OrderStatus;
import com.emerbv.ecommdb.exceptions.ResourceNotFoundException;
import com.emerbv.ecommdb.model.CustomerPaymentMethod;
import com.emerbv.ecommdb.model.Order;
import com.emerbv.ecommdb.model.PaymentTransaction;
import com.emerbv.ecommdb.model.User;
import com.emerbv.ecommdb.repository.CustomerPaymentMethodRepository;
import com.emerbv.ecommdb.repository.OrderRepository;
import com.emerbv.ecommdb.repository.PaymentTransactionRepository;
import com.emerbv.ecommdb.request.PaymentRequest;
import com.emerbv.ecommdb.response.PaymentIntentResponse;
import com.emerbv.ecommdb.util.StripeUtils;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class PaymentService implements IPaymentService {

    private final OrderRepository orderRepository;
    private final PaymentTransactionRepository transactionRepository;
    private final CustomerPaymentMethodRepository customerPaymentMethodRepository;
    private final StripeUtils stripeUtils;

    @Override
    @Transactional
    public PaymentIntentResponse createPaymentIntent(PaymentRequest paymentRequest) throws StripeException {
        // Obtener el orden que queremos pagar
        Order order = orderRepository.findById(paymentRequest.getOrderId())
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + paymentRequest.getOrderId()));

        // Convertir el monto al formato de Stripe (centavos/peniques)
        Long amount = stripeUtils.convertAmountToStripeFormat(order.getTotalAmount());

        // Crear PaymentIntent con configuración básica primero
        Map<String, Object> params = new HashMap<>();
        params.put("amount", amount);
        params.put("currency", paymentRequest.getCurrency() != null ? paymentRequest.getCurrency() : "usd");

        // Configuración específica para aplicaciones móviles
        // Esta configuración evita métodos de pago que requieren redirección
        Map<String, Object> paymentMethodOptions = new HashMap<>();
        Map<String, Object> cardOptions = new HashMap<>();
        cardOptions.put("request_three_d_secure", "automatic");
        paymentMethodOptions.put("card", cardOptions);
        params.put("payment_method_options", paymentMethodOptions);

        // Solo permitir tarjetas como método de pago
        List<String> paymentMethodTypes = List.of("card");
        params.put("payment_method_types", paymentMethodTypes);

        // Metadatos para referencia
        Map<String, String> metadata = new HashMap<>();
        metadata.put("orderId", order.getOrderId().toString());
        params.put("metadata", metadata);

        // Descripción del pago
        params.put("description", "Payment for Order #" + order.getOrderId());

        // Si se proporciona email para recibo
        if (paymentRequest.getReceiptEmail() != null) {
            params.put("receipt_email", paymentRequest.getReceiptEmail());
        }

        // Si se proporciona un método de pago específico
        if (paymentRequest.getPaymentMethodId() != null) {
            params.put("payment_method", paymentRequest.getPaymentMethodId());
        }

        // Crear el PaymentIntent
        PaymentIntent intent = PaymentIntent.create(params);

        // La orden permanece en PENDING hasta que el pago sea confirmado
        // No cambiamos el estado aquí, sino que esperamos la confirmación de Stripe

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
                    order.setOrderStatus(OrderStatus.PAID);
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
