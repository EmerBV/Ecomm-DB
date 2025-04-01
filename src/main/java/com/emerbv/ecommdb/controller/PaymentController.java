package com.emerbv.ecommdb.controller;

import com.emerbv.ecommdb.dto.OrderDto;
import com.emerbv.ecommdb.exceptions.ResourceNotFoundException;
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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RequiredArgsConstructor
@RestController
@RequestMapping("${api.prefix}/payments")
public class PaymentController {

    private final IPaymentService paymentService;
    private final IOrderService orderService;
    private final StripeUtils stripeUtils;

    /*
    @PostMapping("/checkout/order/{orderId}")
    public ResponseEntity<ApiResponse> checkoutOrder(@PathVariable Long orderId, @RequestBody PaymentRequest paymentRequest) {
        try {
            // Asegurarse de que el orderId en el path y en el request coincidan
            paymentRequest.setOrderId(orderId);

            // Obtener información de la orden para mostrarla en la respuesta
            OrderDto orderDto = orderService.getOrder(orderId);

            // Crear el intent de pago
            PaymentIntentResponse paymentIntentResponse = paymentService.createPaymentIntent(paymentRequest);

            // Crear un objeto para la respuesta que incluya tanto el intent de pago como los detalles de la orden
            Map<String, Object> responseData = new HashMap<>();
            responseData.put("paymentIntent", paymentIntentResponse);
            responseData.put("order", orderDto);

            return ResponseEntity.ok(new ApiResponse("Checkout initialized successfully", responseData));
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ApiResponse(e.getMessage(), null));
        } catch (StripeException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse("Payment Intent creation failed", e.getMessage()));
        }
    }
     */

    @PostMapping("/checkout/order/{orderId}")
    public ResponseEntity<ApiResponse> checkoutOrder(@PathVariable Long orderId, @RequestBody PaymentRequest paymentRequest) {
        try {
            // Asegurarse de que el orderId en el path y en el request coincidan
            paymentRequest.setOrderId(orderId);

            // Obtener información de la orden para mostrarla en la respuesta
            OrderDto orderDto = orderService.getOrder(orderId);

            // Crear el intent de pago
            PaymentIntentResponse paymentIntentResponse = paymentService.createPaymentIntent(paymentRequest);

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

    private final StripeWebhookService webhookService;

    // Endpoint para manejar webhooks de Stripe
    @PostMapping("/webhook")
    public ResponseEntity<String> handleStripeWebhook(@RequestBody String payload, @RequestHeader("Stripe-Signature") String sigHeader) {
        try {
            webhookService.processWebhookEvent(payload, sigHeader);
            return ResponseEntity.ok("Webhook processed successfully");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Webhook processing failed: " + e.getMessage());
        }
    }
}
