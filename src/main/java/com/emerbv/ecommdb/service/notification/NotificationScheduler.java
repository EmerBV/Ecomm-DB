package com.emerbv.ecommdb.service.notification;

import com.emerbv.ecommdb.dto.NotificationDto;
import com.emerbv.ecommdb.enums.NotificationType;
import com.emerbv.ecommdb.model.*;
import com.emerbv.ecommdb.repository.*;
import com.emerbv.ecommdb.service.cart.ICartService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Componente que gestiona las tareas programadas para envío de notificaciones automáticas
 */
@Component
@RequiredArgsConstructor
public class NotificationScheduler {
    private static final Logger logger = LoggerFactory.getLogger(NotificationScheduler.class);

    private final NotificationPreferenceRepository preferenceRepository;
    private final INotificationService notificationService;
    private final CartRepository cartRepository;
    private final ProductRepository productRepository;
    private final WishListRepository wishListRepository;
    private final UserRepository userRepository;
    private final ICartService cartService;
    private final NotificationPreferenceService preferenceService;

    /**
     * Tarea programada para enviar notificaciones de carritos abandonados
     * Se ejecuta cada 6 horas
     */
    @Scheduled(fixedRate = 6 * 60 * 60 * 1000) // 6 horas
    @Transactional(readOnly = true)
    public void scheduledAbandonedCartNotifications() {
        logger.info("Iniciando proceso programado de notificaciones de carritos abandonados");

        // 1. Obtener usuarios que tienen habilitadas las notificaciones de carrito
        List<NotificationPreference> eligibleUsers = preferenceRepository.findByCartRemindersTrue();

        if (eligibleUsers.isEmpty()) {
            logger.info("No hay usuarios elegibles para notificaciones de carrito abandonado");
            return;
        }

        // 2. Definir el período de tiempo para considerar un carrito como abandonado
        // (entre 24h y 72h para no ser intrusivos)
        LocalDateTime cutoffStart = LocalDateTime.now().minusHours(72);
        LocalDateTime cutoffEnd = LocalDateTime.now().minusHours(24);

        int notificationsCount = 0;

        // 3. Para cada usuario elegible, verificar carritos abandonados
        for (NotificationPreference preference : eligibleUsers) {
            try {
                Long userId = preference.getUserId();

                // Obtener el carrito del usuario
                Cart cart = cartRepository.findByUserId(userId);

                // Verificar si el carrito existe y tiene items
                if (cart == null || cart.getItems() == null || cart.getItems().isEmpty()) {
                    continue;
                }

                // Verificar si el carrito no ha sido modificado en el período definido
                // Este campo lastModified debería agregarse a la entidad Cart
                // Por ahora simplemente asumiremos que todos los carritos son candidatos

                // Obtener el usuario
                User user = userRepository.findById(userId).orElse(null);
                if (user == null) {
                    continue;
                }

                // Generar URL con token seguro para recuperar el carrito
                String cartRecoveryUrl = generateCartRecoveryUrl(cart.getId(), userId);

                // Preparar la notificación
                Map<String, Object> variables = new HashMap<>();
                variables.put("userName", user.getFirstName());
                variables.put("cartItems", cartService.convertToDto(cart).getItems());
                variables.put("totalAmount", cart.getTotalAmount());
                variables.put("cartRecoveryUrl", cartRecoveryUrl);

                // Añadir una oferta especial si queremos incentivar la compra
                boolean hasSpecialOffer = new Random().nextBoolean(); // 50% de probabilidad
                if (hasSpecialOffer) {
                    variables.put("hasSpecialOffer", true);
                    variables.put("discountPercentage", 10);
                    variables.put("discountCode", "VUELVE10");
                }

                // Preparar enlaces sociales
                Map<String, String> socialLinks = new HashMap<>();
                socialLinks.put("facebook", "https://facebook.com/emerbvstore");
                socialLinks.put("instagram", "https://instagram.com/emerbvstore");
                socialLinks.put("twitter", "https://twitter.com/emerbvstore");
                variables.put("socialLinks", socialLinks);

                // Agregar URL de unsubscribe
                String unsubscribeToken = preferenceService.generateUnsubscribeToken(userId, "CART");
                variables.put("unsubscribeUrl", "https://emerbv-ecommerce.com/notifications/unsubscribe?token=" + unsubscribeToken);

                // Enviar notificación
                notificationService.sendUserNotification(
                        user,
                        NotificationType.CART_ABANDONED,
                        "¿Olvidaste algo en tu carrito?",
                        user.getPreferredLanguage(),
                        variables
                );

                notificationsCount++;

            } catch (Exception e) {
                logger.error("Error procesando carrito abandonado para usuario {}: {}",
                        preference.getUserId(), e.getMessage(), e);
            }
        }

        logger.info("Proceso de notificaciones de carritos abandonados completado. Se enviaron {} notificaciones",
                notificationsCount);
    }

    /**
     * Tarea programada para enviar notificaciones de productos de nuevo en stock
     * Se ejecuta diariamente a las 9 AM
     */
    @Scheduled(cron = "0 0 9 * * ?") // 9 AM todos los días
    @Transactional(readOnly = true)
    public void scheduledBackInStockNotifications() {
        logger.info("Iniciando proceso programado de notificaciones de productos en stock");

        // 1. Obtener usuarios que tienen habilitadas las notificaciones de productos
        List<NotificationPreference> eligibleUsers = preferenceRepository.findByProductUpdatesTrueAndNotificationsEnabledTrue();

        if (eligibleUsers.isEmpty()) {
            logger.info("No hay usuarios elegibles para notificaciones de productos en stock");
            return;
        }

        // 2. Obtener productos que volvieron a tener stock en las últimas 24 horas
        // Esta lógica depende de cómo se actualice el inventario y se registren los cambios
        // Idealmente, tendríamos un registro de cambios de inventario
        // Por ahora, simulamos esto con productos que tienen inventario > 0

        List<Product> backInStockProducts = productRepository.findByStatus(com.emerbv.ecommdb.enums.ProductStatus.IN_STOCK);

        if (backInStockProducts.isEmpty()) {
            logger.info("No hay productos nuevamente en stock");
            return;
        }

        int notificationsCount = 0;

        // 3. Para cada usuario elegible, buscar productos en su wishlist que estén de nuevo en stock
        for (NotificationPreference preference : eligibleUsers) {
            try {
                Long userId = preference.getUserId();

                // Buscar la wishlist del usuario
                Optional<WishList> optionalWishList = wishListRepository.findByUserId(userId);
                if (optionalWishList.isEmpty() || optionalWishList.get().getProducts().isEmpty()) {
                    continue;
                }

                WishList wishList = optionalWishList.get();

                // Obtener productos que están tanto en la wishlist como en los que volvieron a tener stock
                Set<Product> productsToNotify = wishList.getProducts().stream()
                        .filter(backInStockProducts::contains)
                        .collect(Collectors.toSet());

                if (productsToNotify.isEmpty()) {
                    continue;
                }

                // Obtener el usuario
                User user = userRepository.findById(userId).orElse(null);
                if (user == null) {
                    continue;
                }

                // Para cada producto, enviar una notificación
                for (Product product : productsToNotify) {
                    try {
                        // Preparar la notificación
                        Map<String, Object> variables = new HashMap<>();
                        variables.put("userName", user.getFirstName());
                        variables.put("productName", product.getName());
                        variables.put("productPrice", product.getPrice());
                        variables.put("productUrl", "https://emerbv-ecommerce.com/products/" + product.getId());

                        // Imagen del producto si está disponible
                        if (product.getImages() != null && !product.getImages().isEmpty()) {
                            variables.put("productImage", product.getImages().get(0).getDownloadUrl());
                        }

                        // Verificar si el producto tiene descuento
                        if (product.getDiscountPercentage() > 0) {
                            variables.put("hasDiscount", true);
                            variables.put("discountPercentage", product.getDiscountPercentage());
                        }

                        // Preparar enlaces sociales
                        Map<String, String> socialLinks = new HashMap<>();
                        socialLinks.put("facebook", "https://facebook.com/emerbvstore");
                        socialLinks.put("instagram", "https://instagram.com/emerbvstore");
                        socialLinks.put("twitter", "https://twitter.com/emerbvstore");
                        variables.put("socialLinks", socialLinks);

                        // Agregar URL de unsubscribe
                        String unsubscribeToken = preferenceService.generateUnsubscribeToken(userId, "PRODUCT");
                        variables.put("unsubscribeUrl", "https://emerbv-ecommerce.com/notifications/unsubscribe?token=" + unsubscribeToken);

                        // Enviar notificación
                        notificationService.sendUserNotification(
                                user,
                                NotificationType.PRODUCT_BACK_IN_STOCK,
                                "¡" + product.getName() + " ya está disponible!",
                                user.getPreferredLanguage(),
                                variables
                        );

                        notificationsCount++;
                    } catch (Exception e) {
                        logger.error("Error enviando notificación para producto {} al usuario {}: {}",
                                product.getId(), userId, e.getMessage());
                    }
                }

            } catch (Exception e) {
                logger.error("Error procesando notificaciones de productos en stock para usuario {}: {}",
                        preference.getUserId(), e.getMessage(), e);
            }
        }

        logger.info("Proceso de notificaciones de productos en stock completado. Se enviaron {} notificaciones",
                notificationsCount);
    }

    // Métodos de ayuda

    /**
     * Genera una URL para recuperar un carrito abandonado
     */
    private String generateCartRecoveryUrl(Long cartId, Long userId) {
        // Generar un token seguro que expire
        // Idealmente, sería un JWT firmado
        String token = UUID.randomUUID().toString();

        // En una implementación real, guardaríamos este token en la base de datos
        // asociado al carrito y con una fecha de expiración

        return "https://emerbv-ecommerce.com/cart/recover?token=" + token;
    }
}
