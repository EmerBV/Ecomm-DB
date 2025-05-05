package com.emerbv.ecommdb.controller;

import com.emerbv.ecommdb.dto.NotificationPreferenceDto;
import com.emerbv.ecommdb.exceptions.ResourceNotFoundException;
import com.emerbv.ecommdb.model.NotificationPreference;
import com.emerbv.ecommdb.model.User;
import com.emerbv.ecommdb.response.ApiResponse;
import com.emerbv.ecommdb.service.notification.NotificationPreferenceService;
import com.emerbv.ecommdb.service.user.IUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;

/**
 * Controlador para gestionar las preferencias de notificación de los usuarios
 */
@RestController
@RequestMapping("${api.prefix}/notifications/preferences")
@RequiredArgsConstructor
public class NotificationPreferencesController {

    private final NotificationPreferenceService preferenceService;
    private final IUserService userService;

    /**
     * Obtiene las preferencias de notificación para un usuario
     */
    @GetMapping("/user/{userId}")
    @PreAuthorize("hasRole('ROLE_ADMIN') or #userId == authentication.principal.id")
    public ResponseEntity<ApiResponse> getUserPreferences(@PathVariable Long userId) {
        try {
            List<NotificationPreference> preferences = preferenceService.getUserPreferences(userId);
            return ResponseEntity.ok(new ApiResponse("Preferencias de notificación", preferences));
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ApiResponse(e.getMessage(), null));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse("Error al obtener preferencias: " + e.getMessage(), null));
        }
    }

    /**
     * Actualiza las preferencias de notificación para un usuario
     */
    @PutMapping("/user/{userId}")
    @PreAuthorize("hasRole('ROLE_ADMIN') or #userId == authentication.principal.id")
    public ResponseEntity<ApiResponse> updateUserPreferences(
            @PathVariable Long userId,
            @Valid @RequestBody NotificationPreferenceDto preferenceDto) {

        try {
            // Verificar que el usuario existe
            User user = userService.getUserById(userId);

            // Actualizar o crear las preferencias
            NotificationPreference updatedPreference = preferenceService.updatePreference(userId, preferenceDto);

            return ResponseEntity.ok(new ApiResponse("Preferencias actualizadas correctamente", updatedPreference));
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ApiResponse(e.getMessage(), null));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse("Error al actualizar preferencias: " + e.getMessage(), null));
        }
    }

    /**
     * Actualiza una categoría específica de preferencias
     */
    @PutMapping("/user/{userId}/category/{category}")
    @PreAuthorize("hasRole('ROLE_ADMIN') or #userId == authentication.principal.id")
    public ResponseEntity<ApiResponse> updateCategoryPreference(
            @PathVariable Long userId,
            @PathVariable String category,
            @RequestParam boolean enabled) {

        try {
            NotificationPreference preference = preferenceService.updateCategoryPreference(
                    userId, category, enabled);

            return ResponseEntity.ok(new ApiResponse(
                    "Preferencia para " + category + " actualizada", preference));
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ApiResponse(e.getMessage(), null));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse("Error al actualizar preferencia: " + e.getMessage(), null));
        }
    }

    /**
     * Actualiza el canal preferido para las notificaciones
     */
    @PutMapping("/user/{userId}/channel")
    @PreAuthorize("hasRole('ROLE_ADMIN') or #userId == authentication.principal.id")
    public ResponseEntity<ApiResponse> updatePreferredChannel(
            @PathVariable Long userId,
            @RequestParam String channel) {

        try {
            NotificationPreference preference = preferenceService.updatePreferredChannel(userId, channel);

            return ResponseEntity.ok(new ApiResponse(
                    "Canal preferido actualizado a " + channel, preference));
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ApiResponse(e.getMessage(), null));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiResponse(e.getMessage(), null));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse("Error al actualizar canal: " + e.getMessage(), null));
        }
    }

    /**
     * Desactiva todas las notificaciones para un usuario
     */
    @PutMapping("/user/{userId}/unsubscribe-all")
    @PreAuthorize("hasRole('ROLE_ADMIN') or #userId == authentication.principal.id")
    public ResponseEntity<ApiResponse> unsubscribeFromAll(
            @PathVariable Long userId,
            @RequestParam(required = false) String reason) {

        try {
            preferenceService.unsubscribeFromAll(userId, reason);

            return ResponseEntity.ok(new ApiResponse(
                    "Te has dado de baja de todas las notificaciones correctamente", null));
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ApiResponse(e.getMessage(), null));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse("Error al procesar la baja: " + e.getMessage(), null));
        }
    }

    /**
     * Endpoint para manejar links de unsubscribe desde emails (no requiere autenticación)
     */
    @GetMapping("/unsubscribe")
    public ResponseEntity<ApiResponse> handleUnsubscribeLink(
            @RequestParam String token,
            @RequestParam(required = false) String type) {

        try {
            Long userId = preferenceService.verifyUnsubscribeToken(token);

            if (type != null && !type.isEmpty()) {
                // Desuscribir solo de un tipo específico
                preferenceService.unsubscribeFromCategory(userId, type);
                return ResponseEntity.ok(new ApiResponse(
                        "Te has dado de baja de las notificaciones de " + type + " correctamente", null));
            } else {
                // Desuscribir de todo
                preferenceService.unsubscribeFromAll(userId, "Unsubscribe link from email");
                return ResponseEntity.ok(new ApiResponse(
                        "Te has dado de baja de todas las notificaciones correctamente", null));
            }
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiResponse("Token inválido o expirado", null));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse("Error al procesar la baja: " + e.getMessage(), null));
        }
    }
}
