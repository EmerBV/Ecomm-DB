package com.emerbv.ecommdb.service.product;

import com.emerbv.ecommdb.dto.ProductDto;
import com.emerbv.ecommdb.model.Product;
import com.emerbv.ecommdb.request.AddProductRequest;
import com.emerbv.ecommdb.request.ProductUpdateRequest;

import java.util.List;

public interface IProductService {
    Product addProduct(AddProductRequest product);
    Product getProductById(Long id);
    void deleteProductById(Long id);
    Product updateProduct(ProductUpdateRequest product, Long productId);
    List<Product> getAllProducts();
    List<Product> getProductsByCategory(String category);
    List<Product>getProductsByBrand(String brand);
    List<Product> getProductsByCategoryAndBrand(String category, String brand);
    List<Product> getProductsByPrice(Double price);
    List<Product> getProductsByPriceRange(Double minPrice, Double maxPrice);
    List<Product>getProductsByName(String name);
    List<Product>getProductsByBrandAndName(String brand, String name);
    Long countProductsByBrandAndName(String brand, String name);

    List<ProductDto> getConvertedProducts(List<Product> products);
    ProductDto convertToDto(Product product);

    //List<Product> findDistinctProductsByName();

    //List<String> getAllDistinctBrands();
}
