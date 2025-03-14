package com.emerbv.ecommdb.service.variant;

import com.emerbv.ecommdb.dto.VariantDto;
import com.emerbv.ecommdb.model.Variant;
import com.emerbv.ecommdb.request.VariantRequest;

import java.util.List;

public interface IVariantService {
    Variant getVariantById(Long id);
    void deleteVariantById(Long id);
    Variant addVariant(VariantRequest request, Long productId);
    List<VariantDto> saveVariants(Long productId, List<VariantDto> variantsDto);
    Variant updateVariant(VariantRequest request, Long variantId);
    VariantDto convertVariantToDto(Variant variant);
}
