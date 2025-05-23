package com.emerbv.ecommdb.service.notification;

import com.emerbv.ecommdb.dto.CartDto;
import com.emerbv.ecommdb.dto.ProductDto;
import com.emerbv.ecommdb.enums.NotificationType;
import com.emerbv.ecommdb.model.Cart;
import com.emerbv.ecommdb.model.Product;
import com.emerbv.ecommdb.model.User;
import com.emerbv.ecommdb.repository.CartRepository;
import com.emerbv.ecommdb.repository.ProductRepository;
import com.emerbv.ecommdb.repository.UserRepository;
import com.emerbv.ecommdb.service.cart.ICartService;
import com.emerbv.ecommdb.service.product.IProductService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Servicio para manejar campañas automatizadas de notificaciones
 */
@Service
public class NotificationCampaignService {
    private static final Logger logger = LoggerFactory.getLogger(NotificationCampaignService.class);

    private final INotificationService notificationService;
    private final CartRepository cartRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final ICartService cartService;
    private final IProductService productService;
    private final NotificationPreferenceService preferenceService;

    @Autowired
    public NotificationCampaignService(
            INotificationService notificationService,
            CartRepository cartRepository,
            UserRepository userRepository,
            ProductRepository productRepository,
            ICartService cartService,
            IProductService productService,
            NotificationPreferenceService preferenceService) {
        this.notificationService = notificationService;
        this.cartRepository = cartRepository;
        this.userRepository = userRepository;
        this.productRepository = productRepository;
        this.cartService = cartService;
        this.productService = productService;
        this.preferenceService = preferenceService;
    }

    /**
     * Tarea programada para enviar notificaciones de carritos abandonados
     * Se ejecuta cada 6 horas
     */
    @Scheduled(fixedRate = 6 * 60 * 60 * 1000) // 6 horas
    @Transactional(readOnly = true)
    public void sendAbandonedCartNotifications() {
        logger.info("Iniciando proceso de notificaciones de carritos abandonados");

        // Obtener carritos con items que no han tenido actividad en las últimas 24 horas
        // pero menos de 72 horas (para no spam)
        LocalDateTime cutoffStart = LocalDateTime.now().minusHours(72);
        LocalDateTime cutoffEnd = LocalDateTime.now().minusHours(24);

        List<Cart> abandonedCarts = cartRepository.findAll().stream()
                .filter(cart -> !cart.getItems().isEmpty())
                .filter(cart -> {
                    // Filtrar por última actividad - este campo debería añadirse a la entidad Cart
                    // Por ahora usamos updatedAt si existe en la entidad Auditable
                    return true; // Modificar con la lógica real
                })
                .collect(Collectors.toList());

        logger.info("Se encontraron {} carritos abandonados", abandonedCarts.size());

        for (Cart cart : abandonedCarts) {
            try {
                User user = cart.getUser();

                // Verificar si el usuario existe y tiene email
                if (user == null || user.getEmail() == null || user.getEmail().isEmpty()) {
                    continue;
                }

                // Generar URL con token seguro para recuperar el carrito
                String cartRecoveryUrl = generateCartRecoveryUrl(cart.getId(), user.getId());

                // Preparar la notificación
                Map<String, Object> variables = new HashMap<>();
                
                // Variables básicas
                variables.put("userName", user.getFirstName());
                variables.put("cartItems", cart.getItems().stream()
                    .map(item -> {
                        Map<String, Object> itemMap = new HashMap<>();
                        Map<String, Object> productMap = new HashMap<>();
                        productMap.put("name", item.getProduct().getName());
                        productMap.put("images", item.getProduct().getImages().stream()
                            .map(image -> {
                                Map<String, String> imageMap = new HashMap<>();
                                imageMap.put("downloadUrl", image.getDownloadUrl());
                                return imageMap;
                            })
                            .toList());
                        itemMap.put("product", productMap);
                        itemMap.put("variantName", item.getVariantName());
                        itemMap.put("quantity", item.getQuantity());
                        itemMap.put("totalPrice", item.getTotalPrice());
                        return itemMap;
                    })
                    .toList());
                variables.put("totalAmount", cart.getTotalAmount());
                variables.put("cartRecoveryUrl", cartRecoveryUrl);

                // Añadir una oferta especial si queremos incentivar la compra
                boolean hasSpecialOffer = new Random().nextBoolean(); // 50% de probabilidad
                if (hasSpecialOffer) {
                    variables.put("hasSpecialOffer", true);
                    variables.put("discountPercentage", 10);
                    variables.put("discountCode", "VUELVE10");
                }

                // Información de la tienda
                variables.put("storeName", "APPECOMM");
                variables.put("storeEmail", "support@appecomm.com");
                variables.put("storePhone", "+34 123 456 789");
                variables.put("year", java.time.Year.now().getValue());

                // Enlaces sociales
                Map<String, String> socialLinks = new HashMap<>();
                socialLinks.put("facebook", "https://facebook.com/appecomm");
                socialLinks.put("instagram", "https://instagram.com/appecomm");
                socialLinks.put("twitter", "https://twitter.com/appecomm");
                variables.put("socialLinks", socialLinks);

                // URL de cancelación de suscripción
                String unsubscribeToken = preferenceService.generateUnsubscribeToken(user.getId(), "CART");
                variables.put("unsubscribeUrl", "https://appecomm.com/notifications/unsubscribe?token=" + unsubscribeToken);

                // Enviar la notificación
                notificationService.sendUserNotification(
                        user,
                        NotificationType.CART_ABANDONED,
                        "¿Olvidaste algo en tu carrito?",
                        user.getPreferredLanguage(),
                        variables
                );

                logger.info("Notificación de carrito abandonado enviada a usuario: {}", user.getEmail());

            } catch (Exception e) {
                logger.error("Error procesando carrito abandonado {}: {}", cart.getId(), e.getMessage(), e);
            }
        }

        logger.info("Proceso de notificaciones de carritos abandonados completado");
    }

    /**
     * Tarea programada para enviar notificaciones de productos de nuevo en stock
     * Se ejecuta diariamente
     */
    @Scheduled(cron = "0 0 8 * * ?") // 8 AM todos los días
    @Transactional(readOnly = true)
    public void sendBackInStockNotifications() {
        logger.info("Iniciando proceso de notificaciones de productos en stock");

        // Obtener productos que volvieron a tener stock en las últimas 24 horas
        LocalDateTime cutoffTime = LocalDateTime.now().minusHours(24);
        List<Product> backInStockProducts = productRepository.findAll().stream()
                .filter(product -> product.getInventory() > 0)
                .filter(product -> product.getLastStockUpdate() != null && 
                        product.getLastStockUpdate().isAfter(cutoffTime))
                .collect(Collectors.toList());

        for (Product product : backInStockProducts) {
            try {
                // Obtener usuarios que tienen este producto en su lista de deseos
                List<User> interestedUsers = userRepository.findAll().stream()
                        .filter(user -> user.getWishList() != null && 
                                user.getWishList().containsProduct(product.getId()))
                        .collect(Collectors.toList());

                for (User user : interestedUsers) {
                    Map<String, Object> variables = new HashMap<>();
                    
                    // Variables básicas
                    variables.put("userName", user.getFirstName());
                    variables.put("productName", product.getName());
                    variables.put("productUrl", "https://emerbv-ecommerce.com/products/" + product.getId());
                    
                    // Imagen del producto si está disponible
                    if (product.getImages() != null && !product.getImages().isEmpty()) {
                        variables.put("productImage", product.getImages().get(0).getDownloadUrl());
                    }
                    
                    // Precios
                    variables.put("price", product.getPrice());
                    variables.put("inventory", product.getInventory());

                    // Información de la tienda
                    variables.put("storeName", "APPECOMM");
                    variables.put("storeEmail", "support@appecomm.com");
                    variables.put("storePhone", "+34 123 456 789");
                    variables.put("year", java.time.Year.now().getValue());

                    // Enlaces sociales
                    Map<String, String> socialLinks = new HashMap<>();
                    socialLinks.put("facebook", "https://facebook.com/appecomm");
                    socialLinks.put("instagram", "https://instagram.com/appecomm");
                    socialLinks.put("twitter", "https://twitter.com/appecomm");
                    variables.put("socialLinks", socialLinks);

                    // URL de cancelación de suscripción
                    String unsubscribeToken = preferenceService.generateUnsubscribeToken(user.getId(), "BACK_IN_STOCK");
                    variables.put("unsubscribeUrl", "https://appecomm.com/notifications/unsubscribe?token=" + unsubscribeToken);

                    notificationService.sendUserNotification(
                            user,
                            NotificationType.PRODUCT_BACK_IN_STOCK,
                            "¡" + product.getName() + " ya está disponible!",
                            user.getPreferredLanguage(),
                            variables
                    );

                    logger.info("Notificación de producto en stock enviada a usuario: {} para producto: {}", 
                            user.getEmail(), product.getName());
                }
            } catch (Exception e) {
                logger.error("Error procesando notificación de producto en stock {}: {}", 
                        product.getId(), e.getMessage(), e);
            }
        }

        logger.info("Proceso de notificaciones de productos en stock completado");
    }

    /**
     * Tarea programada para enviar recomendaciones personalizadas
     * Se ejecuta semanalmente
     */
    @Scheduled(cron = "0 0 10 ? * SAT") // 10 AM los sábados
    @Transactional(readOnly = true)
    public void sendPersonalizedRecommendations() {
        logger.info("Iniciando proceso de recomendaciones personalizadas");

        List<User> activeUsers = userRepository.findAll(); // Filtrar por usuarios activos

        for (User user : activeUsers) {
            try {
                // Aquí iría un servicio de recomendación que analizaría historial, categorías, etc.
                List<ProductDto> recommendedProducts = getRecommendedProducts(user, 3);

                if (recommendedProducts.isEmpty()) {
                    continue;
                }

                // Para cada producto recomendado, enviar una notificación individual
                for (ProductDto product : recommendedProducts) {
                    Map<String, Object> variables = new HashMap<>();
                    
                    // Variables básicas
                    variables.put("userName", user.getFirstName());
                    variables.put("productName", product.getName());
                    variables.put("productUrl", "https://emerbv-ecommerce.com/products/" + product.getId());
                    
                    // Imagen del producto si está disponible
                    if (product.getImages() != null && !product.getImages().isEmpty()) {
                        variables.put("productImage", product.getImages().get(0).getDownloadUrl());
                    }
                    
                    // Precios y descuentos
                    BigDecimal originalPrice = product.getPrice();
                    BigDecimal discountPercentage = BigDecimal.valueOf(product.getDiscountPercentage());
                    BigDecimal discountAmount = originalPrice.multiply(discountPercentage).divide(BigDecimal.valueOf(100));
                    BigDecimal discountedPrice = originalPrice.subtract(discountAmount);
                    
                    variables.put("originalPrice", originalPrice);
                    variables.put("discountedPrice", discountedPrice);
                    variables.put("discountPercentage", product.getDiscountPercentage());
                    
                    // Tiempo restante de la oferta (24 horas)
                    variables.put("timeRemaining", "24 horas");

                    // Información de la tienda
                    variables.put("storeName", "APPECOMM");
                    variables.put("storeEmail", "support@appecomm.com");
                    variables.put("storePhone", "+34 123 456 789");
                    variables.put("year", java.time.Year.now().getValue());

                    // Enlaces sociales
                    Map<String, String> socialLinks = new HashMap<>();
                    socialLinks.put("facebook", "https://facebook.com/emerbv");
                    socialLinks.put("instagram", "https://instagram.com/emerbv");
                    socialLinks.put("twitter", "https://twitter.com/emerbv");
                    variables.put("socialLinks", socialLinks);

                    // URL de cancelación de suscripción
                    String unsubscribeToken = preferenceService.generateUnsubscribeToken(user.getId(), "RECOMMENDATIONS");
                    variables.put("unsubscribeUrl", "https://emerbv-ecommerce.com/notifications/unsubscribe?token=" + unsubscribeToken);

                    notificationService.sendUserNotification(
                            user,
                            NotificationType.SPECIAL_OFFER,
                            "¡Oferta especial para ti!",
                            user.getPreferredLanguage(),
                            variables
                    );

                    logger.info("Recomendación enviada a usuario: {} para producto: {}", user.getEmail(), product.getName());
                }

            } catch (Exception e) {
                logger.error("Error enviando recomendaciones a usuario {}: {}", user.getId(), e.getMessage(), e);
            }
        }

        logger.info("Proceso de recomendaciones personalizadas completado");
    }

    // Métodos de ayuda

    /**
     * Genera una URL para recuperar un carrito abandonado
     */
    private String generateCartRecoveryUrl(Long cartId, Long userId) {
        // Generar un token seguro que expire
        String token = UUID.randomUUID().toString(); // Simplificado, debería ser un JWT con firma

        String baseUrl = "https://emerbv-ecommerce.com";
        return baseUrl + "/cart/recover?token=" + token;
    }

    /**
     * Obtiene productos recomendados para un usuario
     * Implementación conceptual que se reemplazaría con un algoritmo real
     */
    private List<ProductDto> getRecommendedProducts(User user, int count) {
        // Esta sería una implementación mucho más sofisticada
        // Por ahora solo devolvemos algunos productos de ejemplo

        List<Product> topProducts = productRepository.findTop10ByOrderBySalesCountDesc();

        if (topProducts.isEmpty()) {
            return Collections.emptyList();
        }

        // Limitar al número solicitado y convertir a DTOs
        return topProducts.stream()
                .limit(count)
                .map(productService::convertToDto)
                .collect(Collectors.toList());
    }
}
