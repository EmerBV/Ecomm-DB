package com.emerbv.ecommdb.service.product;

import com.emerbv.ecommdb.dto.ProductDto;
import com.emerbv.ecommdb.enums.ProductStatus;
import com.emerbv.ecommdb.model.Product;
import com.emerbv.ecommdb.request.ProductRequest;
import com.emerbv.ecommdb.dto.ProductFilterDto;
import org.springframework.data.domain.Page;

import java.util.List;

public interface IProductService {
    // Product CRUD operations in ProductService
    Product addProduct(ProductRequest product);
    Product getProductById(Long id);
    void deleteProductById(Long id);
    Product updateProduct(ProductRequest product, Long productId);
    void updateProductAfterVariantsChange(Product product);

    // Product retrieval operations in ProductController
    List<Product> getAllProducts();
    List<Product> getProductsByCategory(String category);
    List<Product>getProductsByBrand(String brand);
    List<Product> getProductsByCategoryAndBrand(String category, String brand);
    List<Product>getProductsByName(String name);
    List<Product>getProductsByBrandAndName(String brand, String name);
    Long countProductsByBrandAndName(String brand, String name);

    // DTO conversion
    List<ProductDto> getConvertedProducts(List<Product> products);
    ProductDto convertToDto(Product product);

    // Specialized queries
    List<Product> getProductsByStatus(ProductStatus status);
    List<Product> getProductsBySalesCount();
    List<Product> getProductsByWishCount();
    List<Product> getRecentProducts();

    // Pre-order specific queries
    List<Product> getPreOrderProducts();
    List<Product> getPreOrderProductsByStatus(ProductStatus status);

    List<Product> getMostWishedProducts(int limit);

    /**
     * Obtiene productos filtrados y ordenados según los criterios especificados
     * @param filterDto DTO con los criterios de filtrado y ordenamiento
     * @return Página de productos filtrados
     */
    Page<Product> getFilteredProducts(ProductFilterDto filterDto);

}
