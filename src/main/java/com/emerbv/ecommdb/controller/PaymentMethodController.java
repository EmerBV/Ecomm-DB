package com.emerbv.ecommdb.controller;

import com.emerbv.ecommdb.dto.PaymentMethodDto;
import com.emerbv.ecommdb.exceptions.ResourceNotFoundException;
import com.emerbv.ecommdb.response.ApiResponse;
import com.emerbv.ecommdb.service.payment.IPaymentMethodService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Controlador para gestionar los métodos de pago de los usuarios
 */
@RequiredArgsConstructor
@RestController
@RequestMapping("${api.prefix}/payment-methods")
public class PaymentMethodController {

    private final IPaymentMethodService paymentMethodService;

    /**
     * Guarda un nuevo método de pago para un usuario
     */
    @PostMapping("/user/{userId}")
    public ResponseEntity<ApiResponse> savePaymentMethod(
            @PathVariable Long userId,
            @RequestBody Map<String, Object> requestBody
    ) {
        String paymentMethodId = (String) requestBody.get("paymentMethodId");
        boolean setAsDefault = requestBody.containsKey("setDefault")
                ? (boolean) requestBody.get("setDefault") : false;

        try {
            PaymentMethodDto savedMethod = paymentMethodService.savePaymentMethod(
                    userId, paymentMethodId, setAsDefault);
            return ResponseEntity.ok(
                    new ApiResponse("Método de pago guardado correctamente", savedMethod));
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ApiResponse(e.getMessage(), null));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse("Error al guardar el método de pago: " + e.getMessage(), null));
        }
    }

    /**
     * Obtiene todos los métodos de pago de un usuario
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<ApiResponse> getUserPaymentMethods(@PathVariable Long userId) {
        try {
            List<PaymentMethodDto> paymentMethods = paymentMethodService.getUserPaymentMethods(userId);
            return ResponseEntity.ok(
                    new ApiResponse("Métodos de pago recuperados correctamente", paymentMethods));
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ApiResponse(e.getMessage(), null));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse("Error al recuperar los métodos de pago: " + e.getMessage(), null));
        }
    }

    /**
     * Establece un método de pago como predeterminado
     */
    @PutMapping("/user/{userId}/default/{paymentMethodId}")
    public ResponseEntity<ApiResponse> setDefaultPaymentMethod(
            @PathVariable Long userId,
            @PathVariable Long paymentMethodId
    ) {
        try {
            PaymentMethodDto updatedMethod = paymentMethodService.setDefaultPaymentMethod(userId, paymentMethodId);
            return ResponseEntity.ok(
                    new ApiResponse("Método de pago predeterminado actualizado", updatedMethod));
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ApiResponse(e.getMessage(), null));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse("Error al actualizar el método de pago predeterminado: " + e.getMessage(), null));
        }
    }

    /**
     * Elimina un método de pago
     */
    @DeleteMapping("/user/{userId}/{paymentMethodId}")
    public ResponseEntity<ApiResponse> deletePaymentMethod(
            @PathVariable Long userId,
            @PathVariable Long paymentMethodId
    ) {
        try {
            paymentMethodService.deletePaymentMethod(userId, paymentMethodId);
            return ResponseEntity.ok(
                    new ApiResponse("Método de pago eliminado correctamente", null));
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ApiResponse(e.getMessage(), null));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse("Error al eliminar el método de pago: " + e.getMessage(), null));
        }
    }

    /**
     * Obtiene el método de pago predeterminado del usuario
     */
    @GetMapping("/user/{userId}/default")
    public ResponseEntity<ApiResponse> getDefaultPaymentMethod(@PathVariable Long userId) {
        try {
            Optional<PaymentMethodDto> defaultMethod = paymentMethodService.getDefaultPaymentMethod(userId);

            if (defaultMethod.isPresent()) {
                return ResponseEntity.ok(
                        new ApiResponse("Método de pago predeterminado recuperado", defaultMethod.get()));
            } else {
                return ResponseEntity.ok(
                        new ApiResponse("El usuario no tiene un método de pago predeterminado", null));
            }
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ApiResponse(e.getMessage(), null));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse("Error al recuperar el método de pago predeterminado: " + e.getMessage(), null));
        }
    }
}
