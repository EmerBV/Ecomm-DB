package com.emerbv.ecommdb.controller;

import com.emerbv.ecommdb.exceptions.ResourceNotFoundException;
import com.emerbv.ecommdb.request.DisputeEvidenceRequest;
import com.emerbv.ecommdb.response.ApiResponse;
import com.emerbv.ecommdb.response.DisputeResponse;
import com.emerbv.ecommdb.service.payment.IDisputeService;
import com.stripe.exception.StripeException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RequiredArgsConstructor
@RestController
@RequestMapping("${api.prefix}/disputes")
public class DisputeController {

    private final IDisputeService disputeService;

    @GetMapping
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<ApiResponse> getAllDisputes() {
        List<DisputeResponse> disputes = disputeService.getAllDisputes();
        return ResponseEntity.ok(new ApiResponse("Disputas encontradas", disputes));
    }

    @GetMapping("/{disputeId}")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<ApiResponse> getDisputeById(@PathVariable Long disputeId) {
        try {
            DisputeResponse dispute = disputeService.getDisputeById(disputeId);
            return ResponseEntity.ok(new ApiResponse("Disputa encontrada", dispute));
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ApiResponse(e.getMessage(), null));
        }
    }

    @GetMapping("/order/{orderId}")
    public ResponseEntity<ApiResponse> getDisputesByOrderId(@PathVariable Long orderId) {
        List<DisputeResponse> disputes = disputeService.getDisputesByOrderId(orderId);
        return ResponseEntity.ok(new ApiResponse("Disputas encontradas para la orden", disputes));
    }

    @PostMapping("/{disputeId}/evidence")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<ApiResponse> submitDisputeEvidence(
            @PathVariable Long disputeId,
            @RequestBody DisputeEvidenceRequest evidence) {
        try {
            DisputeResponse dispute = disputeService.submitDisputeEvidence(disputeId, evidence);
            return ResponseEntity.ok(new ApiResponse("Evidencia enviada correctamente", dispute));
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ApiResponse(e.getMessage(), null));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiResponse(e.getMessage(), null));
        } catch (StripeException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse("Error al enviar evidencia: " + e.getMessage(), null));
        }
    }

    @PostMapping(value = "/{disputeId}/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<ApiResponse> uploadDisputeFile(
            @PathVariable Long disputeId,
            @RequestParam("file") MultipartFile file,
            @RequestParam("purpose") String purpose) {
        try {
            String fileId = disputeService.uploadDisputeFile(disputeId, file, purpose);
            return ResponseEntity.ok(new ApiResponse("Archivo subido correctamente", fileId));
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ApiResponse(e.getMessage(), null));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiResponse(e.getMessage(), null));
        } catch (IOException | StripeException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse("Error al subir archivo: " + e.getMessage(), null));
        }
    }
}
