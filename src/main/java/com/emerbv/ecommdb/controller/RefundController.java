package com.emerbv.ecommdb.controller;

import com.emerbv.ecommdb.exceptions.ResourceNotFoundException;
import com.emerbv.ecommdb.model.Refund;
import com.emerbv.ecommdb.request.RefundRequest;
import com.emerbv.ecommdb.response.ApiResponse;
import com.emerbv.ecommdb.response.RefundResponse;
import com.emerbv.ecommdb.service.payment.IRefundService;
import com.stripe.exception.StripeException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RequiredArgsConstructor
@RestController
@RequestMapping("${api.prefix}/refunds")
public class RefundController {

    private final IRefundService refundService;

    @PostMapping("/create")
    public ResponseEntity<ApiResponse> createRefund(@RequestBody RefundRequest request) {
        try {
            RefundResponse refund = refundService.createRefund(request);
            return ResponseEntity.ok(new ApiResponse("Reembolso creado exitosamente", refund));
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ApiResponse(e.getMessage(), null));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiResponse(e.getMessage(), null));
        } catch (StripeException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse("Error al procesar el reembolso: " + e.getMessage(), null));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse("Error inesperado: " + e.getMessage(), null));
        }
    }

    @GetMapping("/{refundId}")
    public ResponseEntity<ApiResponse> getRefund(@PathVariable String refundId) {
        try {
            RefundResponse refund = refundService.getRefund(refundId);
            return ResponseEntity.ok(new ApiResponse("Reembolso encontrado", refund));
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ApiResponse(e.getMessage(), null));
        } catch (StripeException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse("Error al obtener el reembolso: " + e.getMessage(), null));
        }
    }

    @GetMapping("/order/{orderId}")
    public ResponseEntity<ApiResponse> getRefundsByOrder(@PathVariable Long orderId) {
        try {
            List<Refund> refunds = refundService.getRefundsByOrder(orderId);
            return ResponseEntity.ok(new ApiResponse("Reembolsos encontrados", refunds));
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ApiResponse(e.getMessage(), null));
        }
    }

    @PostMapping("/{refundId}/sync")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<ApiResponse> syncRefundStatus(@PathVariable String refundId) {
        try {
            refundService.syncRefundStatus(refundId);
            return ResponseEntity.ok(new ApiResponse("Estado del reembolso sincronizado correctamente", null));
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ApiResponse(e.getMessage(), null));
        } catch (StripeException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse("Error al sincronizar el reembolso: " + e.getMessage(), null));
        }
    }
}
