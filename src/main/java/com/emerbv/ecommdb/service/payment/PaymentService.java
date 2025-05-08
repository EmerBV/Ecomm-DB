package com.emerbv.ecommdb.service.payment;

import com.emerbv.ecommdb.enums.OrderStatus;
import com.emerbv.ecommdb.exceptions.ResourceNotFoundException;
import com.emerbv.ecommdb.model.*;
import com.emerbv.ecommdb.repository.CustomerPaymentMethodRepository;
import com.emerbv.ecommdb.repository.OrderRepository;
import com.emerbv.ecommdb.repository.PaymentTransactionRepository;
import com.emerbv.ecommdb.request.ApplePaySessionRequest;
import com.emerbv.ecommdb.request.PaymentRequest;
import com.emerbv.ecommdb.response.ApplePayMerchantSessionResponse;
import com.emerbv.ecommdb.response.PaymentIntentResponse;
import com.emerbv.ecommdb.util.StripeUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import com.stripe.net.RequestOptions;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class PaymentService implements IPaymentService {
    private static final Logger logger = LoggerFactory.getLogger(PaymentService.class);

    private final IdempotencyService idempotencyService;
    private final OrderRepository orderRepository;
    private final PaymentTransactionRepository transactionRepository;
    private final CustomerPaymentMethodRepository paymentMethodRepository;
    private final StripeUtils stripeUtils;
    private final StripeOperationService stripeOperationService;
    private final ObjectMapper objectMapper;

    @Value("${app.payment.default-currency:eur}")
    private String defaultCurrency;

    @Value("${stripe.apple-pay.merchant-id}")
    private String applePayMerchantId;

    @Value("${stripe.apple-pay.merchant-domain}")
    private String applePayMerchantDomain;

    @Value("${stripe.apple-pay.merchant-display-name}")
    private String applePayMerchantDisplayName;

    @Value("${stripe.apple-pay.certificate-path}")
    private String applePayCertificatePath;

    @Value("${stripe.apple-pay.certificate-password}")
    private String applePayCertificatePassword;

    @Override
    @Transactional
    public PaymentIntentResponse createPaymentIntent(PaymentRequest paymentRequest) throws StripeException {
        // Verificar si ya existe un intento con la misma clave de idempotencia
        String idempotencyKey = paymentRequest.getIdempotencyKey();
        if (idempotencyKey == null) {
            // Si no se proporciona, generar una
            idempotencyKey = idempotencyService.generateIdempotencyKey();
            logger.info("Generando clave de idempotencia: {}", idempotencyKey);
        }

        Optional<IdempotencyRecord> existingRecord = idempotencyService.findByKey(
                idempotencyKey, "PAYMENT_INTENT_CREATE");

        if (existingRecord.isPresent() && "SUCCESS".equals(existingRecord.get().getStatus())) {
            // Si ya existe un intento exitoso, devolver la información existente
            logger.info("Reutilizando PaymentIntent existente para clave de idempotencia: {}", idempotencyKey);
            String paymentIntentId = existingRecord.get().getEntityId();
            PaymentIntent existingIntent = stripeOperationService.retrievePaymentIntent(paymentIntentId);
            return new PaymentIntentResponse(existingIntent.getClientSecret(), existingIntent.getId());
        }

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
                PaymentIntent existingIntent = stripeOperationService.retrievePaymentIntent(
                        existingTransaction.get().getPaymentIntentId());

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
        params.put("currency", paymentRequest.getCurrency() != null ? paymentRequest.getCurrency() : defaultCurrency);

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

        if (paymentRequest.isApplePay() && StringUtils.hasText(paymentRequest.getApplePayToken())) {
            logger.info("Procesando pago con Apple Pay para la orden: {}", paymentRequest.getOrderId());

            // Para Apple Pay, usamos un método diferente
            params.put("payment_method_data", Map.of(
                    "type", "card",
                    "card", Map.of(
                            "token", paymentRequest.getApplePayToken()
                    )
            ));

            // Y confirmamos inmediatamente
            params.put("confirm", true);

            // Añadir método de pago para rastreo
            if (StringUtils.hasText(paymentRequest.getApplePayPaymentMethod())) {
                params.put("payment_method", paymentRequest.getApplePayPaymentMethod());
            }
        }

        // Si se proporcionó un PaymentMethod, añadirlo al intent
        else if (paymentRequest.getPaymentMethodId() != null) {
            params.put("payment_method", paymentRequest.getPaymentMethodId());
        }

        // Si se proporcionó un ID de método de pago guardado, obtenerlo y usarlo
        else if (paymentRequest.getSavedPaymentMethodId() != null) {
            CustomerPaymentMethod savedMethod = paymentMethodRepository.findById(paymentRequest.getSavedPaymentMethodId())
                    .orElseThrow(() -> new ResourceNotFoundException("Saved payment method not found"));

            // Verificar que el método de pago pertenece al usuario de la orden
            if (!savedMethod.getUser().getId().equals(order.getUser().getId())) {
                throw new IllegalStateException("The payment method does not belong to the order user");
            }

            params.put("payment_method", savedMethod.getStripePaymentMethodId());
        }

        // Configuración para confirmar automáticamente si se proporciona un método de pago
        if (paymentRequest.getPaymentMethodId() != null || paymentRequest.getSavedPaymentMethodId() != null) {
            params.put("confirm", true);
        }

        // Configurar para guardar el método de pago si se solicita
        if (paymentRequest.isSavePaymentMethod() && order.getUser() != null) {
            params.put("setup_future_usage", "off_session");

            // Asegurarse de que el usuario tenga un customerID en Stripe
            User user = order.getUser();
            if (user.getStripeCustomerId() == null || user.getStripeCustomerId().isEmpty()) {
                // Esto usualmente lo manejaría el StripeCustomerService, pero aquí hacemos una verificación simple
                throw new IllegalStateException("User does not have a Stripe customer ID. Please create one first.");
            }

            params.put("customer", user.getStripeCustomerId());
        }

        // Añadir idempotencyKey a la solicitud de Stripe
        com.stripe.net.RequestOptions requestOptions = com.stripe.net.RequestOptions.builder()
                .setIdempotencyKey(idempotencyKey)
                .build();

        try {
            // Crear el PaymentIntent con reintentos
            PaymentIntent intent = stripeOperationService.createPaymentIntent(params, requestOptions);

            // Actualizar la orden con el ID del intent
            order.setPaymentIntentId(intent.getId());
            order.setOrderStatus(OrderStatus.PENDING_PAYMENT);
            orderRepository.save(order);

            // Guardar la transacción de pago
            PaymentTransaction transaction = new PaymentTransaction(
                    intent.getId(),
                    order.getTotalAmount(),
                    paymentRequest.getCurrency() != null ? paymentRequest.getCurrency() : defaultCurrency,
                    intent.getStatus(),
                    intent.getPaymentMethod(),
                    order
            );
            transactionRepository.save(transaction);

            // Registrar la operación exitosa
            idempotencyService.recordOperation(
                    idempotencyKey,
                    "PAYMENT_INTENT_CREATE",
                    intent.getId(),
                    "SUCCESS"
            );

            return new PaymentIntentResponse(intent.getClientSecret(), intent.getId());

        } catch (StripeException e) {
            // Registrar el error para esta operación
            idempotencyService.recordOperation(
                    idempotencyKey,
                    "PAYMENT_INTENT_CREATE",
                    paymentRequest.getOrderId().toString(),
                    "ERROR"
            );

            logger.error("Error creating payment intent for order {}: {}", paymentRequest.getOrderId(), e.getMessage());
            throw e;
        }
    }

    @Override
    @Transactional
    public PaymentIntent confirmPayment(String paymentIntentId) throws StripeException {
        // Verificar si ya existe un registro de idempotencia para esta operación
        String idempotencyKey = idempotencyService.generateIdempotencyKey();

        // Recuperar el PaymentIntent
        PaymentIntent intent = stripeOperationService.retrievePaymentIntent(paymentIntentId);

        // Configurar opciones para la confirmación
        Map<String, Object> params = new HashMap<>();

        try {
            // Confirmar el PaymentIntent con reintentos
            PaymentIntent confirmedIntent = stripeOperationService.confirmPaymentIntent(intent, params);

            // Registrar la operación exitosa
            idempotencyService.recordOperation(
                    idempotencyKey,
                    "PAYMENT_INTENT_CONFIRM",
                    paymentIntentId,
                    "SUCCESS"
            );

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

        } catch (StripeException e) {
            // Registrar el error
            idempotencyService.recordOperation(
                    idempotencyKey,
                    "PAYMENT_INTENT_CONFIRM",
                    paymentIntentId,
                    "ERROR"
            );

            logger.error("Error confirming payment intent {}: {}", paymentIntentId, e.getMessage());
            throw e;
        }
    }

    @Override
    @Transactional
    public PaymentIntent cancelPayment(String paymentIntentId) throws StripeException {
        // Generar clave de idempotencia para esta operación
        String idempotencyKey = idempotencyService.generateIdempotencyKey();

        try {
            // Recuperar el PaymentIntent
            PaymentIntent intent = stripeOperationService.retrievePaymentIntent(paymentIntentId);

            // Configurar opciones para la cancelación
            Map<String, Object> params = new HashMap<>();

            // Cancelar el PaymentIntent con reintentos
            PaymentIntent canceledIntent = stripeOperationService.cancelPaymentIntent(intent, params);

            // Registrar la operación exitosa
            idempotencyService.recordOperation(
                    idempotencyKey,
                    "PAYMENT_INTENT_CANCEL",
                    paymentIntentId,
                    "SUCCESS"
            );

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

        } catch (StripeException e) {
            // Registrar el error
            idempotencyService.recordOperation(
                    idempotencyKey,
                    "PAYMENT_INTENT_CANCEL",
                    paymentIntentId,
                    "ERROR"
            );

            logger.error("Error canceling payment intent {}: {}", paymentIntentId, e.getMessage());
            throw e;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public PaymentIntent retrievePayment(String paymentIntentId) throws StripeException {
        try {
            // Recuperar PaymentIntent con reintentos
            PaymentIntent intent = stripeOperationService.retrievePaymentIntent(paymentIntentId);

            // Opcionalmente, actualizar la transacción local si hay cambios en el estado
            transactionRepository.findByPaymentIntentId(paymentIntentId).ifPresent(transaction -> {
                if (!transaction.getStatus().equals(intent.getStatus())) {
                    transaction.setStatus(intent.getStatus());
                    transaction.setUpdatedAt(LocalDateTime.now());
                    transactionRepository.save(transaction);

                    // Si el pago se completó, actualizar también el estado de la orden
                    if ("succeeded".equals(intent.getStatus())) {
                        Order order = transaction.getOrder();
                        if (order != null && order.getOrderStatus() != OrderStatus.PAID) {
                            order.setOrderStatus(OrderStatus.PAID);
                            order.setPaymentMethod(intent.getPaymentMethod());
                            orderRepository.save(order);
                        }
                    }
                }
            });

            return intent;

        } catch (StripeException e) {
            logger.error("Error retrieving payment intent {}: {}", paymentIntentId, e.getMessage());
            throw e;
        }
    }

    @Override
    @Transactional
    public Order updatePaymentDetails(Long orderId, String paymentIntentId, String paymentMethodId) {
        if (!StringUtils.hasText(paymentIntentId)) {
            throw new IllegalArgumentException("Payment Intent ID cannot be empty");
        }

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + orderId));

        order.setPaymentIntentId(paymentIntentId);

        if (StringUtils.hasText(paymentMethodId)) {
            order.setPaymentMethod(paymentMethodId);
        }

        // Si el pago ha sido procesado exitosamente, actualizar el estado
        if (order.getOrderStatus() == OrderStatus.PENDING ||
                order.getOrderStatus() == OrderStatus.PENDING_PAYMENT) {
            try {
                // Usar el servicio con reintentos
                PaymentIntent intent = stripeOperationService.retrievePaymentIntent(paymentIntentId);
                if ("succeeded".equals(intent.getStatus())) {
                    order.setOrderStatus(OrderStatus.PAID);
                }
            } catch (StripeException e) {
                logger.error("Error retrieving payment intent {} for order update: {}",
                        paymentIntentId, e.getMessage());
                // No fallamos la operación, solo logueamos el error
            }
        }

        logger.info("Updated order {} payment details - intent: {}, method: {}, status: {}",
                orderId, paymentIntentId, paymentMethodId, order.getOrderStatus());

        return orderRepository.save(order);
    }

    @Override
    public ApplePayMerchantSessionResponse validateApplePayMerchant(ApplePaySessionRequest request) throws Exception {
        try {
            URL url = new URL(request.getValidationURL());
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setDoOutput(true);

            // Preparar datos para la solicitud
            Map<String, Object> requestData = new HashMap<>();
            requestData.put("merchantIdentifier", applePayMerchantId);
            requestData.put("displayName", applePayMerchantDisplayName);
            requestData.put("initiative", "web");
            requestData.put("initiativeContext", request.getDomain());

            // Enviar la solicitud
            try (OutputStream os = connection.getOutputStream()) {
                byte[] input = objectMapper.writeValueAsBytes(requestData);
                os.write(input, 0, input.length);
            }

            // Verificar respuesta HTTP
            if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                throw new Exception("Error validating Apple Pay merchant: HTTP error code " + connection.getResponseCode());
            }

            // Leer la respuesta
            StringBuilder responseContent = new StringBuilder();
            try (BufferedReader br = new BufferedReader(
                    new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = br.readLine()) != null) {
                    responseContent.append(line);
                }
            }

            // Convertir la respuesta a objeto
            return objectMapper.readValue(responseContent.toString(), ApplePayMerchantSessionResponse.class);
        } catch (Exception e) {
            logger.error("Error validating Apple Pay merchant: {}", e.getMessage(), e);
            throw new Exception("Error validating Apple Pay merchant: " + e.getMessage(), e);
        }
    }
}
