package com.emerbv.ecommdb.service.user;

import com.emerbv.ecommdb.data.RoleRepository;
import com.emerbv.ecommdb.dto.PaymentMethodDto;
import com.emerbv.ecommdb.dto.UserDto;
import com.emerbv.ecommdb.exceptions.AlreadyExistsException;
import com.emerbv.ecommdb.exceptions.ResourceNotFoundException;
import com.emerbv.ecommdb.model.CustomerPaymentMethod;
import com.emerbv.ecommdb.model.Order;
import com.emerbv.ecommdb.model.Role;
import com.emerbv.ecommdb.model.ShippingDetails;
import com.emerbv.ecommdb.model.User;
import com.emerbv.ecommdb.repository.OrderRepository;
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

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserService implements IUserService {
    private final UserRepository userRepository;
    private final OrderRepository orderRepository;
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

        // Filtrar solo las direcciones de envío activas
        if (user.getShippingDetails() != null) {
            List<ShippingDetails> activeShippingDetails = user.getShippingDetails().stream()
                    .filter(ShippingDetails::isActive)
                    .collect(Collectors.toList());
            userDto.setShippingDetails(activeShippingDetails);
        }

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

    @Override
    @Transactional(readOnly = true)
    public List<User> findAllActiveUsers() {
        // Implementación simplificada - en un caso real podríamos
        // filtrar por usuarios que no estén dados de baja, etc.
        return userRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public List<User> findUsersByFilter(String filter) {
        // Implementación simplificada - en un caso real tendríamos una lógica más compleja
        // para interpretar diferentes tipos de filtros

        if (filter == null || filter.isEmpty()) {
            return findAllActiveUsers();
        }

        // Ejemplo de implementación para diferentes tipos de filtros
        if (filter.startsWith("category:")) {
            String category = filter.substring("category:".length());
            return findUsersByCategory(category);
        } else if (filter.startsWith("city:")) {
            String city = filter.substring("city:".length());
            return findUsersByCity(city);
        } else if (filter.startsWith("hasOrdered:")) {
            String productName = filter.substring("hasOrdered:".length());
            return findUsersByPurchasedProduct(productName);
        }

        // Filtro no reconocido, devolver todos los usuarios activos
        return findAllActiveUsers();
    }

    @Override
    @Transactional
    public void verifyUserEmail(Long userId) {
        User user = getUserById(userId);
        user.setEmailVerified(true);
        user.setEmailVerifiedAt(LocalDateTime.now());
        userRepository.save(user);
    }

    @Override
    @Transactional
    public void verifyUserPhone(Long userId) {
        User user = getUserById(userId);
        user.setPhoneVerified(true);
        user.setPhoneVerifiedAt(LocalDateTime.now());
        userRepository.save(user);
    }

    @Override
    @Transactional
    public User updateUserPhone(Long userId, String phoneNumber) {
        User user = getUserById(userId);
        user.setPhoneNumber(phoneNumber);
        user.setPhoneVerified(false); // Resetear la verificación
        return userRepository.save(user);
    }

    @Override
    @Transactional
    public User updateUserLanguage(Long userId, String language) {
        User user = getUserById(userId);
        user.setPreferredLanguage(language);
        return userRepository.save(user);
    }

    @Override
    @Transactional
    public User updatePushToken(Long userId, String token) {
        User user = getUserById(userId);
        user.setPushToken(token);
        return userRepository.save(user);
    }

    @Override
    @Transactional(readOnly = true)
    public List<User> findUsersForCampaign(String category, Integer daysSinceLastOrder, Double minOrderValue) {
        // En una implementación real, esto sería mucho más eficiente con consultas específicas
        // Esta es una implementación simplificada

        List<User> users = findAllActiveUsers();
        List<User> filteredUsers = new ArrayList<>();

        for (User user : users) {
            // Obtener órdenes del usuario
            List<Order> userOrders = orderRepository.findByUserId(user.getId());

            // Filtrar por categoría si es necesario
            if (category != null && !category.isEmpty()) {
                boolean hasOrderedFromCategory = userOrders.stream()
                        .flatMap(order -> order.getOrderItems().stream())
                        .anyMatch(item -> item.getProduct() != null &&
                                item.getProduct().getCategory() != null &&
                                item.getProduct().getCategory().getName().equalsIgnoreCase(category));

                if (!hasOrderedFromCategory) {
                    continue; // Pasar al siguiente usuario
                }
            }

            // Filtrar por días desde la última orden
            if (daysSinceLastOrder != null) {
                LocalDateTime cutoffDate = LocalDateTime.now().minusDays(daysSinceLastOrder);

                boolean hasRecentOrder = userOrders.stream()
                        .anyMatch(order -> order.getOrderDate().isAfter(cutoffDate));

                if (hasRecentOrder) {
                    continue; // Pasar al siguiente usuario
                }
            }

            // Filtrar por valor mínimo de compras
            if (minOrderValue != null) {
                double totalSpend = userOrders.stream()
                        .mapToDouble(order -> order.getTotalAmount().doubleValue())
                        .sum();

                if (totalSpend < minOrderValue) {
                    continue; // Pasar al siguiente usuario
                }
            }

            // Si pasa todos los filtros, añadir a la lista
            filteredUsers.add(user);
        }

        return filteredUsers;
    }

    // Métodos de ayuda

    private List<User> findUsersByCategory(String category) {
        // Implementación simplificada
        // En un caso real, esto sería una consulta específica a la base de datos

        List<User> allUsers = userRepository.findAll();

        return allUsers.stream()
                .filter(user -> {
                    List<Order> userOrders = orderRepository.findByUserId(user.getId());

                    return userOrders.stream()
                            .flatMap(order -> order.getOrderItems().stream())
                            .anyMatch(item -> item.getProduct() != null &&
                                    item.getProduct().getCategory() != null &&
                                    item.getProduct().getCategory().getName().equalsIgnoreCase(category));
                })
                .collect(Collectors.toList());
    }

    private List<User> findUsersByCity(String city) {
        // Implementación simplificada
        // En un caso real, esto sería una consulta específica a la base de datos

        // Asumiendo que la dirección de envío tiene un campo "city"
        return userRepository.findAll().stream()
                .filter(user -> user.getShippingDetails().stream()
                        .anyMatch(address -> city.equalsIgnoreCase(address.getCity())))
                .collect(Collectors.toList());
    }

    private List<User> findUsersByPurchasedProduct(String productName) {
        // Implementación simplificada
        // En un caso real, esto sería una consulta específica a la base de datos

        List<User> allUsers = userRepository.findAll();

        return allUsers.stream()
                .filter(user -> {
                    List<Order> userOrders = orderRepository.findByUserId(user.getId());

                    return userOrders.stream()
                            .flatMap(order -> order.getOrderItems().stream())
                            .anyMatch(item -> item.getProduct() != null &&
                                    item.getProduct().getName().toLowerCase()
                                            .contains(productName.toLowerCase()));
                })
                .collect(Collectors.toList());
    }

}
