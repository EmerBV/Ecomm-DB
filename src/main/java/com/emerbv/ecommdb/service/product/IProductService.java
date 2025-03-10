package com.emerbv.ecommdb.service.product;

import com.emerbv.ecommdb.dto.ProductDto;
import com.emerbv.ecommdb.enums.ProductStatus;
import com.emerbv.ecommdb.model.Product;
import com.emerbv.ecommdb.request.AddProductRequest;
import com.emerbv.ecommdb.request.ProductUpdateRequest;

import java.util.List;

public interface IProductService {
    // ProductService
    Product addProduct(AddProductRequest product);
    Product getProductById(Long id);
    void deleteProductById(Long id);
    Product updateProduct(ProductUpdateRequest product, Long productId);
    void updateProductAfterVariantsChange(Product product);

    // ProductController
    List<Product> getAllProducts();
    List<Product> getProductsByCategory(String category);
    List<Product>getProductsByBrand(String brand);
    List<Product> getProductsByCategoryAndBrand(String category, String brand);
    List<Product>getProductsByName(String name);
    List<Product>getProductsByBrandAndName(String brand, String name);
    Long countProductsByBrandAndName(String brand, String name);
    List<ProductDto> getConvertedProducts(List<Product> products);
    ProductDto convertToDto(Product product);

    List<Product> getProductsByStatus(ProductStatus status);
    List<Product> getProductsBySalesCount();
    List<Product> getProductsByWishCount();
    List<Product> getRecentProducts();

    /*
    List<Product> getProductsByPrice(Double price);
    List<Product> getProductsByPriceRange(Double minPrice, Double maxPrice);
    List<Product> findDistinctProductsByName();
    List<String> getAllDistinctBrands();
     */
}
