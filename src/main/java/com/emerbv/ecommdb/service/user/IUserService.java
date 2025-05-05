package com.emerbv.ecommdb.service.user;

import com.emerbv.ecommdb.dto.UserDto;
import com.emerbv.ecommdb.model.User;
import com.emerbv.ecommdb.request.CreateUserRequest;
import com.emerbv.ecommdb.request.UserUpdateRequest;

import java.util.List;

public interface IUserService {
    User getUserById(Long userId);
    User createUser(CreateUserRequest request);
    User createAdminUser(CreateUserRequest request);
    User updateUser(UserUpdateRequest request, Long userId);
    void deleteUser(Long userId);
    UserDto convertUserToDto(User user);
    User getAuthenticatedUser();

    /**
     * Encuentra todos los usuarios activos
     * @return Lista de usuarios activos
     */
    List<User> findAllActiveUsers();

    /**
     * Encuentra usuarios según un criterio de filtrado
     * @param filter Criterio de filtrado (categoría, ciudad, etc.)
     * @return Lista de usuarios filtrada
     */
    List<User> findUsersByFilter(String filter);

    /**
     * Marca el email de un usuario como verificado
     * @param userId ID del usuario
     */
    void verifyUserEmail(Long userId);

    /**
     * Marca el teléfono de un usuario como verificado
     * @param userId ID del usuario
     */
    void verifyUserPhone(Long userId);

    /**
     * Actualiza el número de teléfono de un usuario
     * @param userId ID del usuario
     * @param phoneNumber Nuevo número de teléfono
     * @return Usuario actualizado
     */
    User updateUserPhone(Long userId, String phoneNumber);

    /**
     * Actualiza el idioma preferido de un usuario
     * @param userId ID del usuario
     * @param language Código de idioma (ej: "es", "en")
     * @return Usuario actualizado
     */
    User updateUserLanguage(Long userId, String language);

    /**
     * Actualiza el token de dispositivo para notificaciones push
     * @param userId ID del usuario
     * @param token Token del dispositivo
     * @return Usuario actualizado
     */
    User updatePushToken(Long userId, String token);

    /**
     * Encuentra usuarios que cumplan ciertos criterios para campañas
     * @param category Categoría de productos (opcional)
     * @param daysSinceLastOrder Días desde la última compra (opcional)
     * @param minOrderValue Valor mínimo de compras (opcional)
     * @return Lista de usuarios que cumplen los criterios
     */
    List<User> findUsersForCampaign(String category, Integer daysSinceLastOrder, Double minOrderValue);
}
