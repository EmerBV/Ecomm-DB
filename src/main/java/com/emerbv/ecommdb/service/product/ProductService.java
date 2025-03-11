package com.emerbv.ecommdb.service.product;

import com.emerbv.ecommdb.dto.ImageDto;
import com.emerbv.ecommdb.dto.ProductDto;
import com.emerbv.ecommdb.enums.ProductStatus;
import com.emerbv.ecommdb.exceptions.AlreadyExistsException;
import com.emerbv.ecommdb.exceptions.ProductNotFoundException;
import com.emerbv.ecommdb.exceptions.ResourceNotFoundException;
import com.emerbv.ecommdb.model.*;
import com.emerbv.ecommdb.repository.*;
import com.emerbv.ecommdb.request.AddProductRequest;
import com.emerbv.ecommdb.request.ProductUpdateRequest;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ProductService implements IProductService  {
    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final ModelMapper modelMapper;
    private final ImageRepository imageRepository;
    private final CartItemRepository cartItemRepository;
    private final OrderItemRepository orderItemRepository;

    @Override
    public Product addProduct(AddProductRequest request) {
        // Check if the category is found in the DB
        // If Yes, set it as the new product category
        // If No, then save it as a new category
        // Then set as the new product category

        if (productExists(request.getName(), request.getBrand())) {
            throw new AlreadyExistsException(request.getBrand() + " "
                    + request.getName() + " already exists, you may update this product instead!");
        }

        /*
        BigDecimal price = Optional.ofNullable(request.getPrice()).orElse(BigDecimal.ZERO);
        request.setPrice(price);

        int inventory = Math.max(request.getInventory(), 0);
        request.setInventory(inventory);
        System.out.println("Cantidad de inventory: " + inventory);

        int discount = Math.max(request.getDiscountPercentage(), 0);
        request.setDiscountPercentage(discount);
         */

        Category category = Optional.ofNullable(categoryRepository.findByName(request.getCategory().getName()))
                .orElseGet(() -> {
                    Category newCategory = new Category(request.getCategory().getName());
                    return categoryRepository.save(newCategory);
                });
        request.setCategory(category);

        return productRepository.save(createProduct(request, category));
    }

    private boolean productExists(String name, String brand) {
        return productRepository.existsByNameAndBrand(name, brand);
    }

    public Product createProduct(AddProductRequest request, Category category) {
        Product product = new Product(
                request.getName(),
                request.getBrand(),
                request.getPrice(),
                request.getInventory(),
                request.getDescription(),
                category,
                request.getDiscountPercentage(),
                request.getStatus(),
                0,
                0,
                request.isPreOrder()
        );

        product.getProductStatus();

        return product;
    }

    @Override
    public Product getProductById(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException("Product not found!"));
    }

    @Override
    public void deleteProductById(Long id) {
        List<CartItem> cartItems = cartItemRepository.findByProductId(id);
        List<OrderItem> orderItems = orderItemRepository.findByProductId(id);
        productRepository.findById(id)
                .ifPresentOrElse(product -> {
                    // Functional approach for category removal
                    Optional.ofNullable(product.getCategory())
                            .ifPresent(category -> category.getProducts().remove(product));
                    product.setCategory(null);

                    // Functional approach for updating cart items
                    cartItems.stream()
                            .peek(cartItem -> cartItem.setProduct(null))
                            .peek(CartItem::setTotalPrice)
                            .forEach(cartItemRepository::save);

                    // Functional approach for updating order items
                    orderItems.stream()
                            .peek(orderItem -> orderItem.setProduct(null))
                            .forEach(orderItemRepository::save);

                    productRepository.delete(product);
                }, () -> {
                    throw new EntityNotFoundException("Product not found!");
                });
    }

    @Override
    public Product updateProduct(ProductUpdateRequest request, Long productId) {
        return productRepository.findById(productId)
                .map(existingProduct -> updateExistingProduct(existingProduct, request))
                .map(productRepository::save)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found!"));
    }

    private Product updateExistingProduct(Product existingProduct, ProductUpdateRequest request) {
        existingProduct.setName(request.getName());
        existingProduct.setBrand(request.getBrand());
        existingProduct.setPrice(request.getPrice());
        existingProduct.setInventory(request.getInventory());
        existingProduct.setDescription(request.getDescription());
        existingProduct.setPreOrder(request.isPreOrder());

        Category category = categoryRepository.findByName(request.getCategory().getName());
        existingProduct.setCategory(category);

        // Validar descuento: evitar negativos
        int discount = Math.max(request.getDiscountPercentage(), 0);
        existingProduct.setDiscountPercentage(discount);

        existingProduct.updateProductDetails();
        ProductStatus productStatus = existingProduct.getProductStatus();
        existingProduct.setStatus(productStatus);

        return existingProduct;
    }

    @Override
    public void updateProductAfterVariantsChange(Product product) {
        productRepository.save(product);
    }

    @Override
    public List<Product> getAllProducts() {
        return productRepository.findAll();
    }

    @Override
    public List<Product> getProductsByCategory(String category) {
        return productRepository.findByCategoryName(category);
    }

    @Override
    public List<Product> getProductsByBrand(String brand) {
        return productRepository.findByBrand(brand);
    }

    @Override
    public List<Product> getProductsByCategoryAndBrand(String category, String brand) {
        return productRepository.findByCategoryNameAndBrand(category, brand);
    }

    @Override
    public List<Product> getProductsByName(String name) {
        return productRepository.findByName(name);
    }

    @Override
    public List<Product> getProductsByBrandAndName(String brand, String name) {
        return productRepository.findByBrandAndName(brand, name);
    }

    @Override
    public Long countProductsByBrandAndName(String brand, String name) {
        return productRepository.countByBrandAndName(brand, name);
    }

    @Override
    public List<ProductDto> getConvertedProducts(List<Product> products) {
        return products.stream().map(this::convertToDto).toList();
    }

    @Override
    public ProductDto convertToDto(Product product) {
        ProductDto productDto = modelMapper.map(product, ProductDto.class);
        List<Image> images = imageRepository.findByProductId(product.getId());
        List<ImageDto> imageDtos = images.stream()
                .map(image -> modelMapper.map(image, ImageDto.class))
                .toList();
        productDto.setImages(imageDtos);
        return productDto;
    }

    @Override
    public List<Product> getProductsByStatus(ProductStatus status) {
        return productRepository.findByStatus(status);
    }

    @Override
    public List<Product> getProductsBySalesCount() {
        return productRepository.findTop10ByOrderBySalesCountDesc();
    }

    @Override
    public List<Product> getProductsByWishCount() {
        return productRepository.findTop10ByOrderByWishCountDesc();
    }

    @Override
    public List<Product> getRecentProducts() {
        LocalDateTime sevenDaysAgo = LocalDateTime.now().minusDays(7);
        return productRepository.findByCreatedAtAfter(sevenDaysAgo);

        // También se puede hacer de esta manera
        /*
        LocalDateTime sevenDaysAgo = LocalDateTime.now().minusDays(7);
        List<Product> recentProducts = productRepository.findByCreatedAtAfter(sevenDaysAgo);
        return recentProducts;
         */
    }

    @Override
    public List<Product> getPreOrderProducts() {
        return productRepository.findByPreOrderTrue();
    }

    @Override
    public List<Product> getPreOrderProductsByStatus(ProductStatus status) {
        return productRepository.findByPreOrderTrueAndStatus(status);
    }

    /*
    @Override
    public List<Product> getProductsByPrice(Double price) {
        return List.of();
    }

    @Override
    public List<Product> getProductsByPriceRange(Double minPrice, Double maxPrice) {
        return List.of();
    }
     */



}
