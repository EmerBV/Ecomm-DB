package com.emerbv.ecommdb.repository;

import com.emerbv.ecommdb.enums.ProductStatus;
import com.emerbv.ecommdb.model.Product;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

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
