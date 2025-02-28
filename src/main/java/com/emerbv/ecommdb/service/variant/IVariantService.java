package com.emerbv.ecommdb.service.variant;

import com.emerbv.ecommdb.dto.VariantDto;
import com.emerbv.ecommdb.model.Variant;

import java.util.List;

public interface IVariantService {
    Variant getVariantById(Long id);
    void deleteVariantById(Long id);
    List<VariantDto> saveVariants(Long productId, List<VariantDto> variantsDto);
    void updateVariant(VariantDto variantDto, Long variantId);
}
