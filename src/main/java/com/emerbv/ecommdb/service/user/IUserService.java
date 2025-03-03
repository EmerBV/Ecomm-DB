package com.emerbv.ecommdb.service.user;

import com.emerbv.ecommdb.dto.UserDto;
import com.emerbv.ecommdb.model.User;
import com.emerbv.ecommdb.request.CreateUserRequest;
import com.emerbv.ecommdb.request.UserUpdateRequest;

public interface IUserService {
    User getUserById(Long userId);
    User createUser(CreateUserRequest request);
    User createAdminUser(CreateUserRequest request);
    User updateUser(UserUpdateRequest request, Long userId);
    void deleteUser(Long userId);
    UserDto convertUserToDto(User user);

    User getAuthenticatedUser();
}
