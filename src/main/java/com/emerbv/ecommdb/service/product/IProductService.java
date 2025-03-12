package com.emerbv.ecommdb.service.product;

import com.emerbv.ecommdb.dto.ProductDto;
import com.emerbv.ecommdb.enums.ProductStatus;
import com.emerbv.ecommdb.model.Product;
import com.emerbv.ecommdb.request.AddProductRequest;
import com.emerbv.ecommdb.request.ProductUpdateRequest;

import java.util.List;

public interface IProductService {
    // Product CRUD operations in ProductService
    Product addProduct(AddProductRequest product);
    Product getProductById(Long id);
    void deleteProductById(Long id);
    Product updateProduct(ProductUpdateRequest product, Long productId);
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

}
