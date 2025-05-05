package com.emerbv.ecommdb.service.notification;

import com.emerbv.ecommdb.dto.NotificationPreferenceDto;
import com.emerbv.ecommdb.exceptions.ResourceNotFoundException;
import com.emerbv.ecommdb.model.NotificationPreference;
import com.emerbv.ecommdb.model.User;
import com.emerbv.ecommdb.repository.NotificationPreferenceRepository;
import com.emerbv.ecommdb.service.user.IUserService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Optional;

/**
 * Servicio para gestionar las preferencias de notificación de los usuarios
 */
@Service
@RequiredArgsConstructor
public class NotificationPreferenceService {
    private static final Logger logger = LoggerFactory.getLogger(NotificationPreferenceService.class);

    private final NotificationPreferenceRepository preferenceRepository;
    private final IUserService userService;

    @Value("${notification.unsubscribe.token.secret:defaultSecretKeyForDevEnvironmentOnly}")
    private String tokenSecret;

    @Value("${notification.unsubscribe.token.expiration:30}")
    private int tokenExpirationDays;

    /**
     * Obtiene las preferencias de notificación de un usuario
     */
    @Transactional(readOnly = true)
    public List<NotificationPreference> getUserPreferences(Long userId) {
        // Verificar si el usuario existe
        userService.getUserById(userId);

        List<NotificationPreference> preferences = preferenceRepository.findByUserId(userId);

        // Si no tiene preferencias, crear con valores por defecto
        if (preferences.isEmpty()) {
            NotificationPreference defaultPreference = createDefaultPreferences(userId);
            preferences = List.of(defaultPreference);
        }

        return preferences;
    }

    /**
     * Actualiza las preferencias de notificación de un usuario
     */
    @Transactional
    public NotificationPreference updatePreference(Long userId, NotificationPreferenceDto dto) {
        // Verificar si el usuario existe
        userService.getUserById(userId);

        // Buscar preferencias existentes o crear nuevas
        NotificationPreference preference = preferenceRepository.findByUserId(userId)
                .stream().findFirst()
                .orElseGet(() -> createDefaultPreferences(userId));

        // Actualizar solo los campos no nulos del DTO
        if (dto.getPreferredChannel() != null) {
            preference.setPreferredChannel(dto.getPreferredChannel());
        }

        if (dto.getOrderNotifications() != null) {
            preference.setOrderNotifications(dto.getOrderNotifications());
        }

        if (dto.getPaymentNotifications() != null) {
            preference.setPaymentNotifications(dto.getPaymentNotifications());
        }

        if (dto.getShippingNotifications() != null) {
            preference.setShippingNotifications(dto.getShippingNotifications());
        }

        if (dto.getPromotionalEmails() != null) {
            preference.setPromotionalEmails(dto.getPromotionalEmails());
        }

        if (dto.getProductUpdates() != null) {
            preference.setProductUpdates(dto.getProductUpdates());
        }

        if (dto.getCartReminders() != null) {
            preference.setCartReminders(dto.getCartReminders());
        }

        if (dto.getPersonalizedRecommendations() != null) {
            preference.setPersonalizedRecommendations(dto.getPersonalizedRecommendations());
        }

        if (dto.getEventInvitations() != null) {
            preference.setEventInvitations(dto.getEventInvitations());
        }

        if (dto.getMaxEmailsPerWeek() != null) {
            preference.setMaxEmailsPerWeek(dto.getMaxEmailsPerWeek());
        }

        if (dto.getPreferredTimeStart() != null) {
            preference.setPreferredTimeStart(dto.getPreferredTimeStart());
        }

        if (dto.getPreferredTimeEnd() != null) {
            preference.setPreferredTimeEnd(dto.getPreferredTimeEnd());
        }

        if (dto.getPreferredLanguage() != null) {
            preference.setPreferredLanguage(dto.getPreferredLanguage());
        }

        if (dto.getNotificationsEnabled() != null) {
            preference.setNotificationsEnabled(dto.getNotificationsEnabled());

            // Si se están desactivando las notificaciones, guardar motivo si existe
            if (!dto.getNotificationsEnabled() && dto.getUnsubscribeReason() != null) {
                preference.setUnsubscribeReason(dto.getUnsubscribeReason());
            }
        }

        // Actualizar fecha de modificación
        preference.updateTimestamp();

        return preferenceRepository.save(preference);
    }

    /**
     * Actualiza el estado de una categoría específica de notificaciones
     */
    @Transactional
    public NotificationPreference updateCategoryPreference(Long userId, String category, boolean enabled) {
        // Verificar si el usuario existe
        userService.getUserById(userId);

        // Buscar preferencias existentes o crear nuevas
        NotificationPreference preference = preferenceRepository.findByUserId(userId)
                .stream().findFirst()
                .orElseGet(() -> createDefaultPreferences(userId));

        // Actualizar la categoría especificada
        switch (category.toLowerCase()) {
            case "order", "orders" -> preference.setOrderNotifications(enabled);
            case "payment", "payments" -> preference.setPaymentNotifications(enabled);
            case "shipping" -> preference.setShippingNotifications(enabled);
            case "promotional", "promotions" -> preference.setPromotionalEmails(enabled);
            case "product", "products" -> preference.setProductUpdates(enabled);
            case "cart", "cart_reminders" -> preference.setCartReminders(enabled);
            case "recommendations" -> preference.setPersonalizedRecommendations(enabled);
            case "event", "events" -> preference.setEventInvitations(enabled);
            default -> throw new IllegalArgumentException("Categoría de notificación no válida: " + category);
        }

        // Actualizar fecha de modificación
        preference.updateTimestamp();

        return preferenceRepository.save(preference);
    }

    /**
     * Actualiza el canal preferido para las notificaciones
     */
    @Transactional
    public NotificationPreference updatePreferredChannel(Long userId, String channel) {
        // Verificar si el usuario existe
        userService.getUserById(userId);

        // Validar el canal
        if (!Arrays.asList("EMAIL", "SMS", "PUSH").contains(channel.toUpperCase())) {
            throw new IllegalArgumentException("Canal no válido. Debe ser EMAIL, SMS o PUSH");
        }

        // Buscar preferencias existentes o crear nuevas
        NotificationPreference preference = preferenceRepository.findByUserId(userId)
                .stream().findFirst()
                .orElseGet(() -> createDefaultPreferences(userId));

        preference.setPreferredChannel(channel.toUpperCase());

        // Actualizar fecha de modificación
        preference.updateTimestamp();

        return preferenceRepository.save(preference);
    }

    /**
     * Desactiva todas las notificaciones para un usuario
     */
    @Transactional
    public void unsubscribeFromAll(Long userId, String reason) {
        // Verificar si el usuario existe
        userService.getUserById(userId);

        // Buscar preferencias existentes o crear nuevas
        NotificationPreference preference = preferenceRepository.findByUserId(userId)
                .stream().findFirst()
                .orElseGet(() -> createDefaultPreferences(userId));

        preference.setNotificationsEnabled(false);

        if (reason != null && !reason.isEmpty()) {
            preference.setUnsubscribeReason(reason);
        }

        // Actualizar fecha de modificación
        preference.updateTimestamp();

        preferenceRepository.save(preference);

        logger.info("Usuario {} se ha dado de baja de todas las notificaciones. Motivo: {}",
                userId, reason != null ? reason : "No especificado");
    }

    /**
     * Desactiva una categoría específica de notificaciones
     */
    @Transactional
    public void unsubscribeFromCategory(Long userId, String category) {
        updateCategoryPreference(userId, category, false);

        logger.info("Usuario {} se ha dado de baja de notificaciones de tipo: {}", userId, category);
    }

    /**
     * Verifica un token de unsubscribe y devuelve el ID de usuario si es válido
     */
    public Long verifyUnsubscribeToken(String token) {
        try {
            SecretKey key = Keys.hmacShaKeyFor(tokenSecret.getBytes(StandardCharsets.UTF_8));

            Jws<Claims> claims = Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(token);

            // Verificar tipo de token
            String tokenType = claims.getBody().get("type", String.class);
            if (!"unsubscribe".equals(tokenType)) {
                throw new IllegalArgumentException("Tipo de token inválido");
            }

            // Extraer ID de usuario
            return claims.getBody().get("userId", Long.class);

        } catch (Exception e) {
            logger.error("Error validando token de unsubscribe: {}", e.getMessage(), e);
            throw new IllegalArgumentException("Token inválido o expirado");
        }
    }

    /**
     * Genera un token de unsubscribe para un usuario
     */
    public String generateUnsubscribeToken(Long userId, String category) {
        SecretKey key = Keys.hmacShaKeyFor(tokenSecret.getBytes(StandardCharsets.UTF_8));

        Date now = new Date();
        Date expiration = Date.from(LocalDateTime.now()
                .plusDays(tokenExpirationDays)
                .atZone(ZoneId.systemDefault())
                .toInstant());

        return Jwts.builder()
                .setSubject("unsubscribe")
                .claim("userId", userId)
                .claim("type", "unsubscribe")
                .claim("category", category)
                .setIssuedAt(now)
                .setExpiration(expiration)
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * Crea preferencias por defecto para un usuario
     */
    private NotificationPreference createDefaultPreferences(Long userId) {
        NotificationPreference preference = new NotificationPreference();
        preference.setUserId(userId);
        preference.setCreatedAt(LocalDateTime.now());

        // Usar preferencias de idioma del usuario si es posible
        try {
            User user = userService.getUserById(userId);
            // Asumiendo que el usuario tiene un campo de idioma preferido
            // preference.setPreferredLanguage(user.getPreferredLanguage());
        } catch (Exception e) {
            // Ignorar errores y usar valores por defecto
        }

        return preference;
    }
}
