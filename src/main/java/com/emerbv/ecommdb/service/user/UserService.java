package com.emerbv.ecommdb.service.user;

import com.emerbv.ecommdb.data.RoleRepository;
import com.emerbv.ecommdb.dto.PaymentMethodDto;
import com.emerbv.ecommdb.dto.UserDto;
import com.emerbv.ecommdb.exceptions.AlreadyExistsException;
import com.emerbv.ecommdb.exceptions.ResourceNotFoundException;
import com.emerbv.ecommdb.model.CustomerPaymentMethod;
import com.emerbv.ecommdb.model.Role;
import com.emerbv.ecommdb.model.User;
import com.emerbv.ecommdb.repository.UserRepository;
import com.emerbv.ecommdb.request.CreateUserRequest;
import com.emerbv.ecommdb.request.UserUpdateRequest;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserService implements IUserService {
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final ModelMapper modelMapper;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional(readOnly = true)
    public User getUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found!"));
    }

    /*
    @Override
    public User createUser(CreateUserRequest request) {
        Role userRole = roleRepository.findByName("ROLE_USER").get();
        return  Optional.of(request)
                .filter(user -> !userRepository.existsByEmail(request.getEmail()))
                .map(req -> {
                    User user = new User();
                    user.setEmail(request.getEmail());
                    user.setPassword(passwordEncoder.encode(request.getPassword()));
                    user.setFirstName(request.getFirstName());
                    user.setLastName(request.getLastName());
                    user.setRoles(Set.of(userRole));
                    return  userRepository.save(user);
                }) .orElseThrow(() -> new AlreadyExistsException("Oops!" + request.getEmail() + "already exists!"));
    }

    @Override
    public User createAdminUser(CreateUserRequest request) {
        Role userRole = roleRepository.findByName("ROLE_ADMIN").get();
        return  Optional.of(request)
                .filter(user -> !userRepository.existsByEmail(request.getEmail()))
                .map(req -> {
                    User user = new User();
                    user.setEmail(request.getEmail());
                    user.setPassword(passwordEncoder.encode(request.getPassword()));
                    user.setFirstName(request.getFirstName());
                    user.setLastName(request.getLastName());
                    user.setRoles(Set.of(userRole));
                    return  userRepository.save(user);
                }) .orElseThrow(() -> new AlreadyExistsException("Oops!" + request.getEmail() + "already exists!"));
    }
     */

    private User createUserWithRole(CreateUserRequest request, String roleName) {
        Role role = roleRepository.findByName(roleName)
                .orElseThrow(() -> new ResourceNotFoundException("Role not found"));

        return Optional.of(request)
                .filter(user -> !userRepository.existsByEmail(request.getEmail()))
                .map(req -> {
                    User user = new User();
                    user.setEmail(request.getEmail());
                    user.setPassword(passwordEncoder.encode(request.getPassword()));
                    user.setFirstName(request.getFirstName());
                    user.setLastName(request.getLastName());
                    user.setRoles(Set.of(role));
                    return userRepository.save(user);
                })
                .orElseThrow(() -> new AlreadyExistsException("Oops! " + request.getEmail() + " already exists!"));
    }

    @Override
    public User createUser(CreateUserRequest request) {
        return createUserWithRole(request, "ROLE_USER");
    }

    @Override
    public User createAdminUser(CreateUserRequest request) {
        return createUserWithRole(request, "ROLE_ADMIN");
    }

    @Override
    public User updateUser(UserUpdateRequest request, Long userId) {
        return  userRepository.findById(userId).map(existingUser -> {
            existingUser.setFirstName(request.getFirstName());
            existingUser.setLastName(request.getLastName());
            return userRepository.save(existingUser);
        }).orElseThrow(() -> new ResourceNotFoundException("User not found!"));
    }

    @Override
    public void deleteUser(Long userId) {
        userRepository.findById(userId)
                .ifPresentOrElse(userRepository::delete, () -> {
                    throw new ResourceNotFoundException("User not found!");
                });
    }

    @Override
    @Transactional(readOnly = true)
    public UserDto convertUserToDto(User user) {
        UserDto userDto = modelMapper.map(user, UserDto.class);

        // Mapeamos los métodos de pago si existen
        if (user.getPaymentMethods() != null && !user.getPaymentMethods().isEmpty()) {
            List<PaymentMethodDto> paymentMethodDtos = user.getPaymentMethods().stream()
                    .map(this::convertPaymentMethodToDto)
                    .collect(Collectors.toList());
            userDto.setPaymentMethods(paymentMethodDtos);
        }

        return userDto;
    }

    /**
     * Convierte un CustomerPaymentMethod a PaymentMethodDto
     */
    private PaymentMethodDto convertPaymentMethodToDto(CustomerPaymentMethod paymentMethod) {
        return modelMapper.map(paymentMethod, PaymentMethodDto.class);
    }

    @Override
    @Transactional(readOnly = true)
    public User getAuthenticatedUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();
        return userRepository.findByEmail(email);
    }

}
