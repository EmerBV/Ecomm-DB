package com.emerbv.ecommdb.controller;

import com.emerbv.ecommdb.dto.OrderDto;
import com.emerbv.ecommdb.exceptions.PaymentException;
import com.emerbv.ecommdb.exceptions.ResourceNotFoundException;
import com.emerbv.ecommdb.model.Order;
import com.emerbv.ecommdb.request.PayPalPaymentRequest;
import com.emerbv.ecommdb.response.ApiResponse;
import com.emerbv.ecommdb.response.PayPalCaptureResponse;
import com.emerbv.ecommdb.response.PayPalPaymentResponse;
import com.emerbv.ecommdb.service.order.IOrderService;
import com.emerbv.ecommdb.service.payment.IPayPalService;
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
@RequestMapping("${api.prefix}/paypal")
public class PayPalController {
    private static final Logger logger = LoggerFactory.getLogger(PayPalController.class);

    private final IPayPalService payPalService;
    private final IOrderService orderService;

    @PostMapping("/create-payment")
    public ResponseEntity<ApiResponse> createPayment(@RequestBody PayPalPaymentRequest request) {
        try {
            // Crear pago PayPal
            PayPalPaymentResponse paymentResponse = payPalService.createPayment(request);

            // Obtener información de la orden para incluir en la respuesta
            OrderDto orderDto = orderService.getOrder(request.getOrderId());

            // Construir respuesta completa
            Map<String, Object> responseData = new HashMap<>();
            responseData.put("paypalOrder", paymentResponse);
            responseData.put("order", orderDto);

            return ResponseEntity.ok(new ApiResponse("Pago PayPal inicializado correctamente", responseData));
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ApiResponse(e.getMessage(), null));
        } catch (PaymentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiResponse("Error al crear pago: " + e.getMessage(), null));
        } catch (Exception e) {
            logger.error("Error al crear pago PayPal: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse("Error al crear pago PayPal", e.getMessage()));
        }
    }

    @PostMapping("/capture-payment")
    public ResponseEntity<ApiResponse> capturePayment(
            @RequestParam String paypalOrderId,
            @RequestParam Long orderId) {
        try {
            // Capturar pago aprobado
            PayPalCaptureResponse captureResponse = payPalService.capturePayment(paypalOrderId, orderId);

            // Obtener información actualizada de la orden
            OrderDto orderDto = orderService.getOrder(orderId);

            // Construir respuesta completa
            Map<String, Object> responseData = new HashMap<>();
            responseData.put("capture", captureResponse);
            responseData.put("order", orderDto);

            return ResponseEntity.ok(new ApiResponse("Pago capturado correctamente", responseData));
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ApiResponse(e.getMessage(), null));
        } catch (PaymentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiResponse("Error al capturar pago: " + e.getMessage(), null));
        } catch (Exception e) {
            logger.error("Error al capturar pago PayPal: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse("Error al capturar pago PayPal", e.getMessage()));
        }
    }

    @GetMapping("/payment-details")
    public ResponseEntity<ApiResponse> getPaymentDetails(@RequestParam String paypalOrderId) {
        try {
            PayPalPaymentResponse paymentDetails = payPalService.getPaymentDetails(paypalOrderId);
            return ResponseEntity.ok(new ApiResponse("Detalles del pago obtenidos correctamente", paymentDetails));
        } catch (PaymentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiResponse("Error al obtener detalles del pago: " + e.getMessage(), null));
        } catch (Exception e) {
            logger.error("Error al obtener detalles del pago PayPal: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse("Error al obtener detalles del pago PayPal", e.getMessage()));
        }
    }

    @GetMapping("/payment-success")
    public ResponseEntity<ApiResponse> handlePaymentSuccess(
            @RequestParam String paypalOrderId,
            @RequestParam String token,
            @RequestParam String PayerID) {
        try {
            // Buscamos la orden asociada al ID de PayPal
            // Aquí necesitarías un método adicional en tu servicio de ordenes
            // para buscar por paymentIntentId
            // Para este ejemplo, usaremos un endpoint donde el frontend debe enviar el orderId

            return ResponseEntity.ok(new ApiResponse("Pago PayPal completado. Por favor ejecuta la captura del pago.",
                    Map.of("paypalOrderId", paypalOrderId, "token", token, "PayerID", PayerID)));
        } catch (Exception e) {
            logger.error("Error en callback de éxito de PayPal: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse("Error en callback de éxito de PayPal", e.getMessage()));
        }
    }

    @GetMapping("/payment-cancel")
    public ResponseEntity<ApiResponse> handlePaymentCancel(
            @RequestParam String token) {
        logger.info("Pago PayPal cancelado: {}", token);
        return ResponseEntity.ok(new ApiResponse("Pago PayPal cancelado", null));
    }

    @PostMapping("/webhook")
    public ResponseEntity<String> handlePayPalWebhook(@RequestBody String payload) {
        logger.info("Webhook de PayPal recibido: {}", payload);
        // Aquí implementarías la lógica para verificar y procesar eventos de PayPal
        // Similar a como lo haces con los webhooks de Stripe
        return ResponseEntity.ok("Webhook procesado correctamente");
    }
}
