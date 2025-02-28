package com.emerbv.ecommdb.controller;

import com.emerbv.ecommdb.dto.VariantDto;
import com.emerbv.ecommdb.response.ApiResponse;
import com.emerbv.ecommdb.service.variant.IVariantService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;

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
}
