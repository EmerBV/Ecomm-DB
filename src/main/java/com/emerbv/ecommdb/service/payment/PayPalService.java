package com.emerbv.ecommdb.service.payment;

import com.emerbv.ecommdb.enums.OrderStatus;
import com.emerbv.ecommdb.exceptions.PaymentException;
import com.emerbv.ecommdb.exceptions.ResourceNotFoundException;
import com.emerbv.ecommdb.model.Order;
import com.emerbv.ecommdb.model.PaymentTransaction;
import com.emerbv.ecommdb.repository.OrderRepository;
import com.emerbv.ecommdb.repository.PaymentTransactionRepository;
import com.emerbv.ecommdb.request.PayPalPaymentRequest;
import com.emerbv.ecommdb.response.PayPalCaptureResponse;
import com.emerbv.ecommdb.response.PayPalPaymentResponse;
import com.emerbv.ecommdb.util.PayPalUtils;
import com.paypal.core.PayPalHttpClient;
import com.paypal.http.HttpResponse;
import com.paypal.orders.*;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PayPalService implements IPayPalService {
    private static final Logger logger = LoggerFactory.getLogger(PayPalService.class);

    private final PayPalHttpClient payPalHttpClient;
    private final OrderRepository orderRepository;
    private final PaymentTransactionRepository transactionRepository;
    private final PayPalUtils payPalUtils;
    private final IdempotencyService idempotencyService;

    @Override
    @Transactional
    public PayPalPaymentResponse createPayment(PayPalPaymentRequest request) {
        try {
            // Verificar si ya existe un intento con la misma clave de idempotencia
            String idempotencyKey = request.getIdempotencyKey();
            if (idempotencyKey == null) {
                idempotencyKey = idempotencyService.generateIdempotencyKey();
                logger.info("Generando clave de idempotencia para PayPal: {}", idempotencyKey);
            }

            // Buscar si ya existe esta operación con la misma clave
            var existingRecord = idempotencyService.findByKey(idempotencyKey, "PAYPAL_ORDER_CREATE");
            if (existingRecord.isPresent() && "SUCCESS".equals(existingRecord.get().getStatus())) {
                logger.info("Reutilizando orden PayPal existente: {}", existingRecord.get().getEntityId());

                // Recuperar orden existente de PayPal
                return getPaymentDetails(existingRecord.get().getEntityId());
            }

            // Obtener la orden local que estamos pagando
            Order localOrder = orderRepository.findById(request.getOrderId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Orden no encontrada con ID: " + request.getOrderId()));

            // Verificar si la orden ya está pagada
            if (OrderStatus.PAID.equals(localOrder.getOrderStatus())) {
                throw new PaymentException("La orden ya ha sido pagada");
            }

            // Crear los detalles del pedido para PayPal
            OrderRequest orderRequest = buildPayPalOrderRequest(localOrder, request);

            // Crear la orden en PayPal
            OrdersCreateRequest createRequest = new OrdersCreateRequest();
            createRequest.prefer("return=representation");
            createRequest.requestBody(orderRequest);

            // Enviar la solicitud a PayPal
            HttpResponse<com.paypal.orders.Order> response = payPalHttpClient.execute(createRequest);
            com.paypal.orders.Order payPalOrder = response.result();

            logger.info("Orden PayPal creada: {}, Estado: {}", payPalOrder.id(), payPalOrder.status());

            // Actualizar la orden local con el ID de la orden de PayPal
            localOrder.setOrderStatus(OrderStatus.PENDING_PAYMENT);
            localOrder.setPaymentMethod("PAYPAL");
            localOrder.setPaymentIntentId(payPalOrder.id());
            orderRepository.save(localOrder);

            // Guardar transacción
            PaymentTransaction transaction = new PaymentTransaction(
                    payPalOrder.id(),
                    localOrder.getTotalAmount(),
                    request.getCurrency(),
                    payPalOrder.status(),
                    "PAYPAL",
                    localOrder
            );
            transactionRepository.save(transaction);

            // Registrar operación exitosa
            idempotencyService.recordOperation(
                    idempotencyKey,
                    "PAYPAL_ORDER_CREATE",
                    payPalOrder.id(),
                    "SUCCESS"
            );

            // Convertir respuesta para el cliente
            PayPalPaymentResponse paymentResponse = new PayPalPaymentResponse();
            paymentResponse.setPayPalOrderId(payPalOrder.id());
            paymentResponse.setStatus(payPalOrder.status());

            // Extraer enlaces importantes (approve y capture)
            payPalOrder.links().forEach(link -> {
                if ("approve".equals(link.rel())) {
                    paymentResponse.setApprovalUrl(link.href());
                }
            });

            return paymentResponse;

        } catch (IOException e) {
            logger.error("Error al crear orden en PayPal: {}", e.getMessage());
            throw new PaymentException("Error al crear orden en PayPal: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public PayPalCaptureResponse capturePayment(String payPalOrderId, Long orderId) {
        try {
            // Generar clave de idempotencia para esta operación
            String idempotencyKey = idempotencyService.generateIdempotencyKey();

            // Buscar si ya existe esta operación
            var existingRecord = idempotencyService.findByKey(
                    idempotencyKey, "PAYPAL_ORDER_CAPTURE");

            if (existingRecord.isPresent() && "SUCCESS".equals(existingRecord.get().getStatus())) {
                logger.info("Reutilizando captura de PayPal existente: {}", existingRecord.get().getEntityId());

                // Podríamos implementar recuperación del resultado anterior
                // Por ahora, continuamos con una nueva captura
            }

            // Obtener la orden local
            Order localOrder = orderRepository.findById(orderId)
                    .orElseThrow(() -> new ResourceNotFoundException("Orden no encontrada con ID: " + orderId));

            // Verificar que la orden tenga el ID de PayPal correcto
            if (!payPalOrderId.equals(localOrder.getPaymentIntentId())) {
                throw new PaymentException("El ID de la orden PayPal no coincide con el registrado");
            }

            // Crear solicitud de captura
            OrdersCaptureRequest captureRequest = new OrdersCaptureRequest(payPalOrderId);
            captureRequest.requestBody(new OrderRequest());

            // Ejecutar la captura
            HttpResponse<com.paypal.orders.Order> response = payPalHttpClient.execute(captureRequest);
            com.paypal.orders.Order capturedOrder = response.result();

            logger.info("Captura completada para orden PayPal: {}, Estado: {}",
                    capturedOrder.id(), capturedOrder.status());

            // Verificar si la captura fue exitosa
            if ("COMPLETED".equals(capturedOrder.status())) {
                // Actualizar la orden local a PAID
                localOrder.setOrderStatus(OrderStatus.PAID);
                orderRepository.save(localOrder);

                // Actualizar transacción
                transactionRepository.findByPaymentIntentId(payPalOrderId)
                        .ifPresent(transaction -> {
                            transaction.setStatus("succeeded");
                            transactionRepository.save(transaction);
                        });
            }

            // Registrar operación exitosa
            idempotencyService.recordOperation(
                    idempotencyKey,
                    "PAYPAL_ORDER_CAPTURE",
                    payPalOrderId,
                    "SUCCESS"
            );

            // Crear respuesta para el cliente
            PayPalCaptureResponse captureResponse = new PayPalCaptureResponse();
            captureResponse.setPayPalOrderId(capturedOrder.id());
            captureResponse.setStatus(capturedOrder.status());

            // Extraer información de captura si está disponible
            if (capturedOrder.purchaseUnits() != null && !capturedOrder.purchaseUnits().isEmpty()) {
                PurchaseUnit purchaseUnit = capturedOrder.purchaseUnits().get(0);
                if (purchaseUnit.payments() != null && purchaseUnit.payments().captures() != null) {
                    List<Capture> captures = purchaseUnit.payments().captures();
                    if (!captures.isEmpty()) {
                        Capture capture = captures.get(0);
                        captureResponse.setCaptureId(capture.id());
                        captureResponse.setCaptureStatus(capture.status());
                        captureResponse.setAmount(new BigDecimal(capture.amount().value()));
                        captureResponse.setCurrency(capture.amount().currencyCode());
                    }
                }
            }

            return captureResponse;

        } catch (IOException e) {
            logger.error("Error al capturar pago en PayPal: {}", e.getMessage());
            throw new PaymentException("Error al capturar pago en PayPal: " + e.getMessage());
        }
    }

    @Override
    public PayPalPaymentResponse getPaymentDetails(String payPalOrderId) {
        try {
            OrdersGetRequest getRequest = new OrdersGetRequest(payPalOrderId);
            HttpResponse<com.paypal.orders.Order> response = payPalHttpClient.execute(getRequest);
            com.paypal.orders.Order payPalOrder = response.result();

            PayPalPaymentResponse paymentResponse = new PayPalPaymentResponse();
            paymentResponse.setPayPalOrderId(payPalOrder.id());
            paymentResponse.setStatus(payPalOrder.status());

            // Extraer enlaces importantes
            payPalOrder.links().forEach(link -> {
                if ("approve".equals(link.rel())) {
                    paymentResponse.setApprovalUrl(link.href());
                }
            });

            // Obtener detalles adicionales
            if (payPalOrder.purchaseUnits() != null && !payPalOrder.purchaseUnits().isEmpty()) {
                PurchaseUnit purchaseUnit = payPalOrder.purchaseUnits().get(0);
                paymentResponse.setReferenceId(purchaseUnit.referenceId());

                if (purchaseUnit.amountWithBreakdown() != null) {
                    paymentResponse.setAmount(new BigDecimal(purchaseUnit.amountWithBreakdown().value()));
                    paymentResponse.setCurrency(purchaseUnit.amountWithBreakdown().currencyCode());
                }
            }

            return paymentResponse;

        } catch (IOException e) {
            logger.error("Error al obtener detalles de orden PayPal: {}", e.getMessage());
            throw new PaymentException("Error al obtener detalles de pago de PayPal: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public Order updatePaymentDetails(Order order, String payPalOrderId, String payPalPayerId) {
        order.setPaymentIntentId(payPalOrderId);

        // El PayerId es importante para identificar quién realizó el pago
        if (payPalPayerId != null) {
            // Podemos almacenarlo en el mismo campo o en uno específico
            order.setPaymentMethod("PAYPAL:" + payPalPayerId);
        } else {
            order.setPaymentMethod("PAYPAL");
        }

        // Verificar si el pago ha sido completado
        try {
            PayPalPaymentResponse paymentDetails = getPaymentDetails(payPalOrderId);
            if ("COMPLETED".equals(paymentDetails.getStatus())) {
                order.setOrderStatus(OrderStatus.PAID);
            } else if ("APPROVED".equals(paymentDetails.getStatus())) {
                order.setOrderStatus(OrderStatus.PENDING_PAYMENT);
            }
        } catch (Exception e) {
            logger.error("Error al verificar estado de pago con PayPal: {}", e.getMessage());
            // No fallamos la operación, solo logueamos el error
        }

        return orderRepository.save(order);
    }

    /**
     * Construye la solicitud de orden para PayPal
     */
    private OrderRequest buildPayPalOrderRequest(Order localOrder, PayPalPaymentRequest request) {
        OrderRequest orderRequest = new OrderRequest();
        orderRequest.checkoutPaymentIntent("CAPTURE");

        // Configurar payer info si está disponible
        if (localOrder.getUser() != null) {
            Payer payer = new Payer();
            payer.email(localOrder.getUser().getEmail());
            orderRequest.payer(payer);
        }

        // Configurar la unidad de compra (detalles del pedido)
        List<PurchaseUnitRequest> purchaseUnits = new ArrayList<>();
        PurchaseUnitRequest purchaseUnit = new PurchaseUnitRequest()
                .referenceId(localOrder.getOrderId().toString())
                .description("Orden #" + localOrder.getOrderId())
                .customId(localOrder.getOrderId().toString());

        // Configurar el monto
        AmountWithBreakdown amount = new AmountWithBreakdown()
                .currencyCode(request.getCurrency())
                .value(localOrder.getTotalAmount().toString());
        purchaseUnit.amountWithBreakdown(amount);

        // Agregar información de envío si está disponible
        if (localOrder.getShippingDetails() != null) {
            ShippingDetail shipping = new ShippingDetail()
                    .name(new Name().fullName(localOrder.getShippingDetails().getFullName()))
                    .addressPortable(new AddressPortable()
                            .addressLine1(localOrder.getShippingDetails().getAddress())
                            .adminArea2(localOrder.getShippingDetails().getCity())
                            .adminArea1(localOrder.getShippingDetails().getState())
                            .postalCode(localOrder.getShippingDetails().getPostalCode())
                            .countryCode(getCountryCode(localOrder.getShippingDetails().getCountry())));

            purchaseUnit.shippingDetail(shipping);
        }

        purchaseUnits.add(purchaseUnit);
        orderRequest.purchaseUnits(purchaseUnits);

        // Configurar experiencia de aplicación
        ApplicationContext appContext = new ApplicationContext()
                .brandName("APPECOMM")
                .landingPage("LOGIN")
                .shippingPreference("SET_PROVIDED_ADDRESS")
                .userAction("PAY_NOW")
                .returnUrl(request.getReturnUrl())
                .cancelUrl(request.getCancelUrl());

        orderRequest.applicationContext(appContext);

        return orderRequest;
    }

    /**
     * Convierte el nombre del país a código ISO de 2 letras
     */
    private String getCountryCode(String country) {
        // Implementación simple, en producción usar una librería de mapeo de países
        if (country == null) return "ES"; // Por defecto España

        switch (country.toLowerCase()) {
            case "españa":
            case "spain":
                return "ES";
            case "estados unidos":
            case "united states":
            case "usa":
                return "US";
            case "méxico":
            case "mexico":
                return "MX";
            default:
                return "ES"; // Valor por defecto
        }
    }
}
