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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class PaymentService implements IPaymentService {

    private static final Logger logger = LoggerFactory.getLogger(PaymentService.class);
    private final OrderRepository orderRepository;
    private final PaymentTransactionRepository transactionRepository;
    private final CustomerPaymentMethodRepository customerPaymentMethodRepository;
    private final StripeUtils stripeUtils;

    @Override
    @Transactional
    public PaymentIntentResponse createPaymentIntent(PaymentRequest paymentRequest) throws StripeException {
        // Obtener la orden que queremos pagar
        Order order = orderRepository.findById(paymentRequest.getOrderId())
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + paymentRequest.getOrderId()));

        // Verificar si la orden ya está pagada
        if (OrderStatus.PAID.equals(order.getOrderStatus())) {
            throw new com.emerbv.ecommdb.exceptions.StripeException("Order is already paid");
        }

        // Buscar si ya existe una transacción para esta orden
        Optional<PaymentTransaction> existingTransaction = transactionRepository.findByOrderOrderId(order.getOrderId())
                .stream()
                .filter(t -> !"canceled".equals(t.getStatus()))
                .findFirst();

        // Si existe una transacción activa, usamos ese PaymentIntent
        if (existingTransaction.isPresent() &&
                !("canceled".equals(existingTransaction.get().getStatus()) ||
                        "failed".equals(existingTransaction.get().getStatus()))) {

            // Verificar con Stripe el estado actual
            try {
                PaymentIntent existingIntent = PaymentIntent.retrieve(existingTransaction.get().getPaymentIntentId());

                // Si el intent no está cancelado o fallido, podemos reutilizarlo
                if (!"canceled".equals(existingIntent.getStatus()) &&
                        !"failed".equals(existingIntent.getStatus())) {

                    return new PaymentIntentResponse(existingIntent.getClientSecret(), existingIntent.getId());
                }
            } catch (Exception e) {
                // Si hay algún error al recuperar el intent, procedemos a crear uno nuevo
                // Solo registramos el error para diagnóstico
                logger.error("Error retrieving existing payment intent: {}", e.getMessage());
            }
        }

        // Si llegamos aquí, necesitamos crear un nuevo PaymentIntent

        // Convertir el monto al formato de Stripe (centavos/peniques)
        Long amount = stripeUtils.convertAmountToStripeFormat(order.getTotalAmount());

        // Crear PaymentIntent con configuración básica
        Map<String, Object> params = new HashMap<>();
        params.put("amount", amount);
        params.put("currency", paymentRequest.getCurrency() != null ? paymentRequest.getCurrency() : "usd");

        // Configuración para métodos de pago
        Map<String, Object> paymentMethodOptions = new HashMap<>();
        Map<String, Object> cardOptions = new HashMap<>();
        cardOptions.put("request_three_d_secure", "automatic");
        paymentMethodOptions.put("card", cardOptions);
        params.put("payment_method_options", paymentMethodOptions);

        List<String> paymentMethodTypes = List.of("card");
        params.put("payment_method_types", paymentMethodTypes);

        // Metadatos para referencia
        Map<String, String> metadata = new HashMap<>();
        metadata.put("orderId", order.getOrderId().toString());
        params.put("metadata", metadata);

        params.put("description", "Payment for Order #" + order.getOrderId());

        if (paymentRequest.getReceiptEmail() != null) {
            params.put("receipt_email", paymentRequest.getReceiptEmail());
        }

        if (paymentRequest.getPaymentMethodId() != null) {
            params.put("payment_method", paymentRequest.getPaymentMethodId());
        }

        // Crear el PaymentIntent
        PaymentIntent intent = PaymentIntent.create(params);

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

                    // Actualizar información de pago en la orden
                    order.setPaymentMethod(confirmedIntent.getPaymentMethod());
                    order.setPaymentIntentId(confirmedIntent.getId());

                    orderRepository.save(order);
                    logger.info("Order {} marked as PAID with payment method {} and intent {}",
                            orderId, confirmedIntent.getPaymentMethod(), confirmedIntent.getId());

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
