package com.emerbv.ecommdb.controller;

import com.emerbv.ecommdb.dto.UserDto;
import com.emerbv.ecommdb.dto.VariantDto;
import com.emerbv.ecommdb.exceptions.ResourceNotFoundException;
import com.emerbv.ecommdb.model.Image;
import com.emerbv.ecommdb.model.User;
import com.emerbv.ecommdb.model.Variant;
import com.emerbv.ecommdb.request.UserUpdateRequest;
import com.emerbv.ecommdb.request.VariantUpdateRequest;
import com.emerbv.ecommdb.response.ApiResponse;
import com.emerbv.ecommdb.service.variant.IVariantService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.sql.SQLException;
import java.util.List;

import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@RequiredArgsConstructor
@RestController
@RequestMapping("${api.prefix}/variants")
public class VariantController {
    private final IVariantService variantService;

    @PostMapping("/variant/add")
    public ResponseEntity<ApiResponse> saveVariants(
            @RequestParam Long productId,
            @RequestBody List<VariantDto> variants
    ) {
        try {
            List<VariantDto> variantsDto = variantService.saveVariants(productId, variants);
            return ResponseEntity.ok(new ApiResponse("Variants added successfully!", variantsDto));
        } catch (Exception e) {
            return ResponseEntity.status(INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse("Add variants failed!", e.getMessage()));
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

    @PutMapping("/variant/{variantId}/update")
    public ResponseEntity<ApiResponse> updateVariant(@RequestBody VariantUpdateRequest request, @PathVariable Long variantId) {
        try {
            Variant variant = variantService.updateVariant(request, variantId);
            VariantDto variantDto = variantService.convertVariantToDto(variant);
            return ResponseEntity.ok(new ApiResponse("Update Variant Success!", variantDto));
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(NOT_FOUND).body(new ApiResponse(e.getMessage(), null));
        }
    }

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
