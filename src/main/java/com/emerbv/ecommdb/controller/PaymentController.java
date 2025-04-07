package com.emerbv.ecommdb.controller;

import com.emerbv.ecommdb.dto.OrderDto;
import com.emerbv.ecommdb.exceptions.ResourceNotFoundException;
import com.emerbv.ecommdb.model.Order;
import com.emerbv.ecommdb.request.PaymentRequest;
import com.emerbv.ecommdb.response.ApiResponse;
import com.emerbv.ecommdb.response.PaymentIntentResponse;
import com.emerbv.ecommdb.service.order.IOrderService;
import com.emerbv.ecommdb.service.payment.IPaymentService;
import com.emerbv.ecommdb.service.payment.StripeWebhookService;
import com.emerbv.ecommdb.util.StripeUtils;
import com.stripe.exception.CardException;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RequiredArgsConstructor
@RestController
@RequestMapping("${api.prefix}/payments")
public class PaymentController {
    private static final Logger logger = LoggerFactory.getLogger(PaymentController.class);

    private final IPaymentService paymentService;
    private final IOrderService orderService;
    private final StripeUtils stripeUtils;
    private final StripeWebhookService webhookService;

    @PostMapping("/checkout/order/{orderId}")
    public ResponseEntity<ApiResponse> checkoutOrder(@PathVariable Long orderId, @RequestBody PaymentRequest paymentRequest) {
        try {
            // Asegurarse de que el orderId en el path y en el request coincidan
            paymentRequest.setOrderId(orderId);

            // Obtener información de la orden para mostrarla en la respuesta
            OrderDto orderDto = orderService.getOrder(orderId);

            // Crear el intent de pago
            PaymentIntentResponse paymentIntentResponse = paymentService.createPaymentIntent(paymentRequest);

            // Registrar el ID del PaymentIntent en la orden para futura referencia
            // Esto garantiza que incluso si el webhook falla, tengamos un registro de la intención de pago
            try {
                Order order = orderService.updatePaymentIntent(orderId, paymentIntentResponse.getPaymentIntentId());
                logger.info("Updated order {} with payment intent {}", orderId, paymentIntentResponse.getPaymentIntentId());

                // Actualizar el DTO para la respuesta
                orderDto = orderService.convertToDto(order);
            } catch (Exception e) {
                logger.error("Failed to update order with payment intent: {}", e.getMessage());
                // No fallar toda la operación si esto no funciona
            }

            // MODIFICACIÓN: Crear un mapa con la estructura esperada por el cliente
            Map<String, Object> responseData = new HashMap<>();
            responseData.put("clientSecret", paymentIntentResponse.getClientSecret());
            responseData.put("paymentIntentId", paymentIntentResponse.getPaymentIntentId());
            responseData.put("order", orderDto);

            return ResponseEntity.ok(new ApiResponse("Checkout initialized successfully", responseData));
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ApiResponse(e.getMessage(), null));
        } catch (StripeException e) {
            // Obtener detalles más específicos del error de Stripe
            String errorMessage = "Payment Intent creation failed";
            String errorDetails = e.getMessage();

            if (e instanceof CardException) {
                CardException cardException = (CardException) e;
                errorMessage = "Card error: " + cardException.getCode();
                errorDetails = cardException.getDeclineCode() != null ?
                        "Decline code: " + cardException.getDeclineCode() : errorDetails;
            }

            Map<String, Object> errorData = new HashMap<>();
            errorData.put("message", errorMessage);
            errorData.put("details", errorDetails);
            errorData.put("code", e.getCode());

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse(errorMessage, errorData));
        }
    }

    @PostMapping("/create-payment-intent")
    public ResponseEntity<ApiResponse> createPaymentIntent(@RequestBody PaymentRequest paymentRequest) {
        try {
            PaymentIntentResponse paymentIntentResponse = paymentService.createPaymentIntent(paymentRequest);

            // Actualizar el registro de la orden con el ID del PaymentIntent
            if (paymentRequest.getOrderId() != null) {
                try {
                    orderService.updatePaymentIntent(paymentRequest.getOrderId(), paymentIntentResponse.getPaymentIntentId());
                } catch (Exception e) {
                    logger.error("Failed to update order with payment intent: {}", e.getMessage());
                    // No fallar toda la operación si esto no funciona
                }
            }

            return ResponseEntity.ok(new ApiResponse("Payment Intent created successfully", paymentIntentResponse));
        } catch (StripeException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse("Payment Intent creation failed", e.getMessage()));
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ApiResponse(e.getMessage(), null));
        }
    }

    @PostMapping("/confirm/{paymentIntentId}")
    public ResponseEntity<ApiResponse> confirmPayment(@PathVariable String paymentIntentId) {
        try {
            PaymentIntent confirmedIntent = paymentService.confirmPayment(paymentIntentId);

            // Usar StripeUtils para convertir el PaymentIntent a un mapa serializable
            Map<String, Object> paymentData = stripeUtils.convertPaymentIntentToMap(confirmedIntent);

            // Si el pago se confirmó con éxito, actualizar la orden
            if ("succeeded".equals(confirmedIntent.getStatus())) {
                try {
                    String orderId = confirmedIntent.getMetadata().get("orderId");
                    if (orderId != null) {
                        // Esta operación ya se realiza en PaymentService, pero la reforzamos aquí
                        Order order = orderService.updatePaymentDetails(
                                Long.parseLong(orderId),
                                confirmedIntent.getId(),
                                confirmedIntent.getPaymentMethod()
                        );

                        // Añadir la orden actualizada a la respuesta
                        paymentData.put("order", orderService.convertToDto(order));
                    }
                } catch (Exception e) {
                    logger.error("Error updating order after payment confirmation: {}", e.getMessage());
                    // No hacer fallar la respuesta si esto falla
                }
            }

            return ResponseEntity.ok(new ApiResponse("Payment confirmed successfully", paymentData));
        } catch (StripeException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse("Payment confirmation failed", e.getMessage()));
        }
    }

    @PostMapping("/cancel/{paymentIntentId}")
    public ResponseEntity<ApiResponse> cancelPayment(@PathVariable String paymentIntentId) {
        try {
            PaymentIntent canceledIntent = paymentService.cancelPayment(paymentIntentId);

            Map<String, Object> paymentData = stripeUtils.convertPaymentIntentToMap(canceledIntent);

            return ResponseEntity.ok(new ApiResponse("Payment canceled successfully", paymentData));
        } catch (StripeException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse("Payment cancellation failed", e.getMessage()));
        }
    }

    @GetMapping("/{paymentIntentId}")
    public ResponseEntity<ApiResponse> retrievePayment(@PathVariable String paymentIntentId) {
        try {
            PaymentIntent intent = paymentService.retrievePayment(paymentIntentId);

            Map<String, Object> paymentData = stripeUtils.convertPaymentIntentToMap(intent);

            return ResponseEntity.ok(new ApiResponse("Payment intent retrieved successfully", paymentData));
        } catch (StripeException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse("Failed to retrieve payment intent", e.getMessage()));
        }
    }

    // Endpoint para manejar webhooks de Stripe
    @PostMapping("/webhook")
    public ResponseEntity<String> handleStripeWebhook(@RequestBody String payload, @RequestHeader("Stripe-Signature") String sigHeader) {
        try {
            webhookService.processWebhookEvent(payload, sigHeader);
            return ResponseEntity.ok("Webhook processed successfully");
        } catch (Exception e) {
            logger.error("Error processing webhook: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Webhook processing failed: " + e.getMessage());
        }
    }
}
