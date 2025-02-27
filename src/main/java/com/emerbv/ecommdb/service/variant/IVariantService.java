package com.emerbv.ecommdb.service.variant;

import com.emerbv.ecommdb.model.Variant;

public interface IVariantService {
    Variant getVariantById(Long id);
    void deleteVariantById(Long id);
}
