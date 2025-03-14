package com.emerbv.ecommdb.service.variant;

import com.emerbv.ecommdb.dto.VariantDto;
import com.emerbv.ecommdb.enums.ProductStatus;
import com.emerbv.ecommdb.exceptions.ResourceNotFoundException;
import com.emerbv.ecommdb.model.Product;
import com.emerbv.ecommdb.model.Variant;
import com.emerbv.ecommdb.repository.VariantRepository;
import com.emerbv.ecommdb.request.VariantRequest;
import com.emerbv.ecommdb.service.product.IProductService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class VariantService implements IVariantService {
    private  final VariantRepository variantRepository;
    private  final IProductService productService;
    private final ModelMapper modelMapper;

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

    @Override
    @Transactional
    public Variant addVariant(VariantRequest request, Long productId) {
        Product product = productService.getProductById(productId);
        Variant variant = createVariant(request, product);

        // Guardar la variante
        Variant savedVariant = variantRepository.save(variant);

        // Actualizar el producto después de añadir la variante
        updateProductAfterVariantChange(product);

        return savedVariant;
    }

    public Variant createVariant(VariantRequest request, Product product) {
        Variant theVariant = new Variant();
        theVariant.setName(request.getName());
        theVariant.setPrice(request.getPrice());
        theVariant.setInventory(request.getInventory());
        theVariant.setProduct(product);

        return theVariant;
    }

    /*
    @Override
    public List<VariantDto> saveVariants(Long productId, List<VariantDto> variantsDto) {
        Product product = productService.getProductById(productId);
        List<VariantDto> savedVariantsDto = new ArrayList<>();

        for (VariantDto variantDto : variantsDto) {
            Variant variant = new Variant();
            variant.setName(variantDto.getName());
            variant.setPrice(variantDto.getPrice());
            variant.setInventory(variantDto.getInventory());
            variant.setProduct(product);

            Variant savedVariant = variantRepository.save(variant);

            // Convertimos a DTO antes de devolver
            VariantDto savedVariantDto = new VariantDto();
            savedVariantDto.setId(savedVariant.getId());
            savedVariantDto.setName(savedVariant.getName());
            savedVariantDto.setPrice(savedVariant.getPrice());
            savedVariantDto.setInventory(savedVariant.getInventory());

            savedVariantsDto.add(savedVariantDto);
        }

        // Actualizar los detalles del producto después de añadir todas las variantes
        product.updateProductDetails();
        product.getProductStatus();
        productService.updateProductAfterVariantsChange(product);

        return savedVariantsDto;
    }
     */


    @Override
    public Variant updateVariant(VariantRequest request, Long variantId) {
        return  variantRepository.findById(variantId).map(existingVariant -> {
            existingVariant.setName(request.getName());
            existingVariant.setPrice(request.getPrice());
            existingVariant.setInventory(request.getInventory());
            Variant savedVariant = variantRepository.save(existingVariant);

            // Actualizar el producto después de modificar la variante
            Product product = existingVariant.getProduct();
            updateProductAfterVariantChange(product);

            return savedVariant;
        }).orElseThrow(() -> new ResourceNotFoundException("Variant not found!"));
    }

    /**
     * Método auxiliar para actualizar todos los detalles del producto después de un cambio en las variantes
     * Garantiza que el precio, inventario y estado se actualicen correctamente
     */
    private void updateProductAfterVariantChange(Product product) {
        product.updateProductDetails();

        // Actualizar el estado del producto basado en el inventario total
        int totalInventory = product.getTotalInventory();
        product.setStatus(totalInventory > 0 ? ProductStatus.IN_STOCK : ProductStatus.OUT_OF_STOCK);

        // Guardar los cambios en el producto
        productService.updateProductAfterVariantsChange(product);
    }

    @Override
    public VariantDto convertVariantToDto(Variant variant) {
        return modelMapper.map(variant, VariantDto.class);
    }
}
