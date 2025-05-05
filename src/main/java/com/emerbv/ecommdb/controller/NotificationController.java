package com.emerbv.ecommdb.controller;

import com.emerbv.ecommdb.dto.NotificationDto;
import com.emerbv.ecommdb.enums.NotificationType;
import com.emerbv.ecommdb.model.Notification;
import com.emerbv.ecommdb.model.User;
import com.emerbv.ecommdb.response.ApiResponse;
import com.emerbv.ecommdb.service.notification.INotificationService;
import com.emerbv.ecommdb.repository.NotificationRepository;
import com.emerbv.ecommdb.service.user.IUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Controlador para la gestión de notificaciones
 */
@RestController
@RequestMapping("${api.prefix}/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final INotificationService notificationService;
    private final NotificationRepository notificationRepository;
    private final IUserService userService;

    /**
     * Envía una notificación personalizada a un usuario específico
     */
    @PostMapping("/users/{userId}/send")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<ApiResponse> sendNotificationToUser(
            @PathVariable Long userId,
            @RequestBody NotificationDto notificationDto) {

        try {
            User user = userService.getUserById(userId);

            // Completar datos del usuario
            notificationDto.setUserId(userId);
            notificationDto.setRecipientEmail(user.getEmail());
            notificationDto.setRecipientName(user.getFirstName() + " " + user.getLastName());

            // Enviar notificación
            notificationService.sendNotification(notificationDto);

            return ResponseEntity.ok(new ApiResponse("Notificación enviada correctamente", null));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse("Error al enviar notificación: " + e.getMessage(), null));
        }
    }

    /**
     * Envía una notificación a todos los usuarios o a un grupo filtrado
     */
    @PostMapping("/broadcast")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<ApiResponse> broadcastNotification(
            @RequestBody NotificationDto template,
            @RequestParam(required = false) String filter) {

        try {
            // Obtener usuarios según el filtro
            List<User> users;

            if (filter != null && !filter.isEmpty()) {
                // Implementar lógica de filtrado
                // Por ejemplo, usuarios que compraron en cierta categoría, etc.
                users = userService.findUsersByFilter(filter);
            } else {
                users = userService.findAllActiveUsers();
            }

            if (users.isEmpty()) {
                return ResponseEntity.ok(new ApiResponse("No hay usuarios que coincidan con el filtro", null));
            }

            // Enviar a cada usuario
            int sentCount = 0;
            for (User user : users) {
                try {
                    Map<String, Object> variables = new HashMap<>(template.getVariables());
                    variables.put("userName", user.getFirstName());

                    notificationService.sendUserNotification(
                            user,
                            template.getType(),
                            template.getSubject(),
                            template.getLanguage(),
                            variables
                    );

                    sentCount++;
                } catch (Exception e) {
                    // Loguear error pero continuar con otros usuarios
                }
            }

            return ResponseEntity.ok(new ApiResponse(
                    "Notificación enviada a " + sentCount + " de " + users.size() + " usuarios",
                    Map.of("totalUsers", users.size(), "successCount", sentCount)
            ));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse("Error en broadcast de notificaciones: " + e.getMessage(), null));
        }
    }

    /**
     * Obtiene el historial de notificaciones de un usuario
     */
    @GetMapping("/users/{userId}/history")
    public ResponseEntity<ApiResponse> getUserNotificationHistory(
            @PathVariable Long userId,
            Pageable pageable) {

        try {
            // Verificar si el usuario existe
            userService.getUserById(userId);

            List<Notification> notifications = notificationRepository.findByUserIdOrderBySentAtDesc(userId);

            return ResponseEntity.ok(new ApiResponse("Historial de notificaciones", notifications));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse("Error obteniendo historial: " + e.getMessage(), null));
        }
    }

    /**
     * Obtiene estadísticas de notificaciones para el panel de administración
     */
    @GetMapping("/stats")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<ApiResponse> getNotificationStats(
            @RequestParam(required = false) LocalDateTime startDate,
            @RequestParam(required = false) LocalDateTime endDate) {

        if (startDate == null) {
            startDate = LocalDateTime.now().minusDays(30);
        }

        if (endDate == null) {
            endDate = LocalDateTime.now();
        }

        try {
            List<Object[]> stats = notificationRepository.countByTypeAndSuccessInPeriod(startDate, endDate);

            // Transformar los resultados a un formato más amigable
            Map<String, Map<String, Long>> formattedStats = new HashMap<>();

            for (Object[] row : stats) {
                String type = (String) row[0];
                boolean success = (boolean) row[1];
                Long count = (Long) row[2];

                formattedStats.computeIfAbsent(type, k -> new HashMap<>())
                        .put(success ? "success" : "failure", count);
            }

            return ResponseEntity.ok(new ApiResponse("Estadísticas de notificaciones", formattedStats));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse("Error obteniendo estadísticas: " + e.getMessage(), null));
        }
    }

    /**
     * Reintentar envío de una notificación fallida
     */
    @PostMapping("/{notificationId}/retry")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<ApiResponse> retryNotification(@PathVariable Long notificationId) {
        try {
            Notification notification = notificationRepository.findById(notificationId)
                    .orElseThrow(() -> new RuntimeException("Notificación no encontrada"));

            if (notification.isSuccess()) {
                return ResponseEntity.badRequest()
                        .body(new ApiResponse("La notificación ya se envió correctamente", null));
            }

            // Crear un DTO basado en la notificación fallida
            NotificationDto dto = NotificationDto.builder()
                    .type(NotificationType.valueOf(notification.getType()))
                    .recipientEmail(notification.getRecipientEmail())
                    .phoneNumber(notification.getRecipientPhone())
                    .subject(notification.getSubject())
                    .userId(notification.getUserId())
                    .build();

            // Intentar enviar nuevamente
            notificationService.sendNotification(dto);

            return ResponseEntity.ok(new ApiResponse("Notificación reenviada correctamente", null));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse("Error al reintentar notificación: " + e.getMessage(), null));
        }
    }

    /**
     * Listar todas las notificaciones (para administración)
     */
    @GetMapping("/list")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<ApiResponse> getAllNotifications(
            Pageable pageable,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String channel,
            @RequestParam(required = false) Boolean success) {

        try {
            Page<Notification> notifications;

            // Filtrar según parámetros
            if (type != null && !type.isEmpty()) {
                notifications = notificationRepository.findByType(type, pageable);
            } else {
                notifications = notificationRepository.findAll(pageable);
            }

            return ResponseEntity.ok(new ApiResponse("Listado de notificaciones", notifications));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse("Error obteniendo notificaciones: " + e.getMessage(), null));
        }
    }
}
