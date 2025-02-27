package com.emerbv.ecommdb.service.variant;

import com.emerbv.ecommdb.exceptions.ResourceNotFoundException;
import com.emerbv.ecommdb.model.Variant;
import com.emerbv.ecommdb.repository.VariantRepository;
import com.emerbv.ecommdb.service.product.IProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class VariantService implements IVariantService {
    private  final VariantRepository variantRepository;
    private  final IProductService productService;

    @Override
    public Variant getVariantById(Long id) {
        return variantRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("No image found with id: " + id));
    }

    @Override
    public void deleteVariantById(Long id) {
        variantRepository.findById(id).ifPresentOrElse(variantRepository::delete, () -> {
            throw new ResourceNotFoundException("No variant found with id: " + id);
        });
    }
}
