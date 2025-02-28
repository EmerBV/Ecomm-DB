package com.emerbv.ecommdb.service.variant;

import com.emerbv.ecommdb.dto.VariantDto;
import com.emerbv.ecommdb.model.Variant;
import com.emerbv.ecommdb.request.VariantUpdateRequest;

import java.util.List;

public interface IVariantService {
    Variant getVariantById(Long id);
    void deleteVariantById(Long id);
    List<VariantDto> saveVariants(Long productId, List<VariantDto> variantsDto);
    Variant updateVariant(VariantUpdateRequest request, Long variantId);
    VariantDto convertVariantToDto(Variant variant);
}
