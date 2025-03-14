package com.emerbv.ecommdb.controller;

import com.emerbv.ecommdb.dto.VariantDto;
import com.emerbv.ecommdb.exceptions.ResourceNotFoundException;
import com.emerbv.ecommdb.model.Variant;
import com.emerbv.ecommdb.request.VariantRequest;
import com.emerbv.ecommdb.response.ApiResponse;
import com.emerbv.ecommdb.service.variant.IVariantService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@RequiredArgsConstructor
@RestController
@RequestMapping("${api.prefix}/variants")
public class VariantController {
    private final IVariantService variantService;

    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @PostMapping("/variant/add")
    public ResponseEntity<ApiResponse> addVariant(
            @Valid @RequestBody VariantRequest variant,
            @RequestParam Long productId
    ) {
        try {
            Variant theVariant = variantService.addVariant(variant, productId);
            VariantDto variantDto = variantService.convertVariantToDto(theVariant);
            return ResponseEntity.ok(new ApiResponse("Variant added successfully!", variantDto));
        } catch (Exception e) {
            return ResponseEntity.status(INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse("Add variant failed!", e.getMessage()));
        }
    }

    @GetMapping("/variant/{variantId}/variant")
    public ResponseEntity<ApiResponse> getVariantById(@PathVariable Long variantId) {
        try {
            Variant variant = variantService.getVariantById(variantId);
            VariantDto variantDto = variantService.convertVariantToDto(variant);
            return ResponseEntity.ok(new ApiResponse("Success", variantDto));
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(NOT_FOUND).body(new ApiResponse(e.getMessage(), null));
        }
    }

    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @PutMapping("/variant/{variantId}/update")
    public ResponseEntity<ApiResponse> updateVariant(
            @RequestBody VariantRequest request,
            @PathVariable Long variantId
    ) {
        try {
            Variant variant = variantService.updateVariant(request, variantId);
            VariantDto variantDto = variantService.convertVariantToDto(variant);
            return ResponseEntity.ok(new ApiResponse("Update Variant Success!", variantDto));
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(NOT_FOUND).body(new ApiResponse(e.getMessage(), null));
        }
    }

    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @DeleteMapping("/variant/{variantId}/delete")
    public ResponseEntity<ApiResponse> deleteVariant(@PathVariable Long variantId) {
        try {
            variantService.deleteVariantById(variantId);
            return ResponseEntity.ok(new ApiResponse("Delete Variant Success!", null));
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(NOT_FOUND).body(new ApiResponse(e.getMessage(), null));
        }
    }
}
