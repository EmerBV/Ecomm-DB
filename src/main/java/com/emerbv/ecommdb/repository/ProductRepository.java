package com.emerbv.ecommdb.repository;

import com.emerbv.ecommdb.enums.ProductStatus;
import com.emerbv.ecommdb.model.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {
    // ProductService
    @EntityGraph(attributePaths = {"category", "images"})
    List<Product> findAll();

    @EntityGraph(attributePaths = {"category", "images"})
    List<Product> findByCategoryName(String category);

    @EntityGraph(attributePaths = {"category", "images"})
    List<Product> findByBrand(String brand);

    @EntityGraph(attributePaths = {"category", "images"})
    List<Product> findByCategoryNameAndBrand(String category, String brand);

    @Query("SELECT p FROM Product p WHERE LOWER(p.name) LIKE LOWER(CONCAT('%', :name, '%'))")
    List<Product> findByName(@Param("name") String name);

    @EntityGraph(attributePaths = {"category", "images"})
    List<Product> findByBrandAndName(String brand, String name);

    Long countByBrandAndName(String brand, String name);

    boolean existsByNameAndBrand(String name, String brand);

    @EntityGraph(attributePaths = {"category", "images"})
    List<Product> findByStatus(ProductStatus status);

    @EntityGraph(attributePaths = {"category", "images"})
    List<Product> findTop10ByOrderBySalesCountDesc();

    @EntityGraph(attributePaths = {"category", "images"})
    List<Product> findTop10ByOrderByWishCountDesc();

    @EntityGraph(attributePaths = {"category", "images"})
    List<Product> findByCreatedAtAfter(LocalDateTime date);

    @EntityGraph(attributePaths = {"category", "images"})
    List<Product> findByPreOrderTrue();

    @EntityGraph(attributePaths = {"category", "images"})
    List<Product> findByPreOrderTrueAndStatus(ProductStatus status);

    @EntityGraph(attributePaths = {"images", "category"})
    @Query("SELECT p FROM Product p WHERE " +
            "(:availability IS NULL OR p.status = :availability) AND " +
            "(:category IS NULL OR p.category.name = :category) AND " +
            "(:brand IS NULL OR p.brand = :brand) AND " +
            "(:minPrice IS NULL OR p.price >= :minPrice) AND " +
            "(:maxPrice IS NULL OR p.price <= :maxPrice)")
    Page<Product> findProductsWithFilters(
            @Param("availability") ProductStatus availability,
            @Param("category") String category,
            @Param("brand") String brand,
            @Param("minPrice") BigDecimal minPrice,
            @Param("maxPrice") BigDecimal maxPrice,
            Pageable pageable);

    @EntityGraph(attributePaths = {"images", "category", "brand"})
    List<Product> findByStatusOrderByCreatedAtDesc(ProductStatus status);

    @EntityGraph(attributePaths = {"images", "category", "brand"})
    List<Product> findByOrderBySalesCountDesc();

    @EntityGraph(attributePaths = {"images", "category", "brand"})
    List<Product> findByOrderByWishCountDesc();

    @EntityGraph(attributePaths = {"images", "category", "brand"})
    List<Product> findByOrderByNameAsc();

    @EntityGraph(attributePaths = {"images", "category", "brand"})
    List<Product> findByOrderByNameDesc();

    @EntityGraph(attributePaths = {"images", "category", "brand"})
    List<Product> findByOrderByPriceDesc();

    @EntityGraph(attributePaths = {"images", "category", "brand"})
    List<Product> findByOrderByPriceAsc();

    @EntityGraph(attributePaths = {"images", "category", "brand"})
    List<Product> findByOrderByDiscountPercentageDesc();

    // TODO
    /*
    // Nuevos métodos basados en el modelo Product:

    // Filtrar productos por descuento (porcentaje)
    List<Product> findByDiscountPercentageGreaterThanEqual(int discountPercentage);

    // Filtrar productos por estado (activo, inactivo, etc.) usando ProductStatus
    List<Product> findByStatus(ProductStatus status);

    // Filtrar productos por el número de ventas (top ventas)
    List<Product> findTop10ByOrderBySalesCountDesc();

    // Filtrar productos por el número de deseos (wishlist)
    List<Product> findByWishCountGreaterThanEqual(int wishCount);

    // Filtrar productos por el estado (activo/inactivo) y el descuento
    List<Product> findByStatusAndDiscountPercentageGreaterThanEqual(ProductStatus status, int discountPercentage);

    // Filtrar productos por fecha de creación (para ver los productos nuevos)
    List<Product> findByCreatedAtAfter(LocalDateTime date);

    // Buscar productos que estén en stock
    List<Product> findByInventoryGreaterThan(int inventory);

    // Filtrar productos por estado (pre-order, disponible, etc.) y en stock
    List<Product> findByStatusAndInventoryGreaterThan(ProductStatus status, int inventory);

    // Filtrar productos por descuento y si están en stock
    List<Product> findByDiscountPercentageGreaterThanEqualAndInventoryGreaterThan(int discountPercentage, int inventory);

    // Paginación para los productos más vendidos
    Page<Product> findTop10ByOrderBySalesCountDesc(Pageable pageable);

    // Filtrar productos por categoría
    List<Product> findByCategoryName(String category);
     */


}
