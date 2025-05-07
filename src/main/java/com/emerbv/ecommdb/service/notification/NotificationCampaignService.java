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
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Servicio para manejar campañas automatizadas de notificaciones
 */
@Service
@RequiredArgsConstructor
public class NotificationCampaignService {
    private static final Logger logger = LoggerFactory.getLogger(NotificationCampaignService.class);

    private final INotificationService notificationService;
    private final CartRepository cartRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final ICartService cartService;
    private final IProductService productService;
    private final NotificationPreferenceService preferenceService;

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
        // Esta lógica depende de cómo se actualice el inventario
        // Aquí se necesitaría un modelo de WishList o similar

        // Ejemplo conceptual:
        /*
        List<Product> backInStockProducts = productRepository.findProductsBackInStock(LocalDateTime.now().minusDays(1));

        for (Product product : backInStockProducts) {
            List<User> interestedUsers = wishListRepository.findUsersByProductId(product.getId());

            for (User user : interestedUsers) {
                Map<String, Object> variables = new HashMap<>();
                variables.put("userName", user.getFirstName());
                variables.put("productName", product.getName());
                variables.put("productUrl", "/products/" + product.getId());
                variables.put("productImage", product.getImages().isEmpty() ? "" : product.getImages().get(0).getDownloadUrl());

                notificationService.sendUserNotification(
                    user,
                    NotificationType.PRODUCT_BACK_IN_STOCK,
                    "¡" + product.getName() + " ya está disponible!",
                    "es",
                    variables
                );
            }
        }
        */

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

        // Lógica para generar recomendaciones basadas en compras anteriores
        // De nuevo, esta es una implementación conceptual

        List<User> activeUsers = userRepository.findAll(); // Filtrar por usuarios activos

        for (User user : activeUsers) {
            try {
                // Aquí iría un servicio de recomendación que analizaría historial, categorías, etc.
                List<ProductDto> recommendedProducts = getRecommendedProducts(user, 3);

                if (recommendedProducts.isEmpty()) {
                    continue;
                }

                Map<String, Object> variables = new HashMap<>();
                variables.put("userName", user.getFirstName());
                variables.put("recommendedProducts", recommendedProducts);

                notificationService.sendUserNotification(
                        user,
                        NotificationType.SPECIAL_OFFER,
                        "Productos seleccionados para ti",
                        "templates/notifications/es",
                        variables
                );

                logger.info("Recomendaciones enviadas a usuario: {}", user.getEmail());

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
