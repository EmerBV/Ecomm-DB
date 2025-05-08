package com.emerbv.ecommdb.service.shipping;

import com.emerbv.ecommdb.exceptions.AlreadyExistsException;
import com.emerbv.ecommdb.exceptions.ResourceNotFoundException;
import com.emerbv.ecommdb.model.ShippingDetails;
import com.emerbv.ecommdb.model.User;
import com.emerbv.ecommdb.repository.ShippingDetailsRepository;
import com.emerbv.ecommdb.repository.UserRepository;
import com.emerbv.ecommdb.request.ShippingDetailsRequest;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ShippingService implements IShippingService {
    private final UserRepository userRepository;
    private final ShippingDetailsRepository shippingDetailsRepository;

    @Override
    @Transactional
    public ShippingDetails addOrUpdateShippingDetails(ShippingDetailsRequest request, User user) {
        try {
            // Si se está actualizando una dirección existente
            if (request.getId() != null) {
                ShippingDetails existingDetails = shippingDetailsRepository.findById(request.getId())
                        .orElseThrow(() -> new ResourceNotFoundException("Shipping details not found with id: " + request.getId()));

                // Luego verificamos si el usuario es propietario
                if (!existingDetails.getUser().getId().equals(user.getId())) {
                    throw new AccessDeniedException("You don't have permission to update this shipping address");
                }

                // Si todo está bien, actualizamos
                updateShippingDetailsFields(existingDetails, request);

                return shippingDetailsRepository.save(existingDetails);
            }

            // Si es una nueva dirección
            ShippingDetails newDetails = new ShippingDetails();
            newDetails.setUser(user);
            newDetails.setActive(true);
            updateShippingDetailsFields(newDetails, request);

            // Si es la primera dirección o está marcada como predeterminada
            if (request.isDefault() || getShippingDetailsByUserId(user.getId()).isEmpty()) {
                // Desactivar cualquier otra dirección predeterminada
                setAllShippingDetailsNonDefault(user.getId());
                newDetails.setDefault(true);
            }

            return shippingDetailsRepository.save(newDetails);
        } catch (Exception e) {
            throw e; // Re-lanzar para manejarlo en el controlador
        }
    }

    private void updateShippingDetailsFields(ShippingDetails details, ShippingDetailsRequest request) {
        // Solo actualizar campos que no sean nulos
        if (request.getAddress() != null) {
            details.setAddress(request.getAddress());
        }
        if (request.getCity() != null) {
            details.setCity(request.getCity());
        }
        if (request.getState() != null) {
            details.setState(request.getState());
        }
        if (request.getPostalCode() != null) {
            details.setPostalCode(request.getPostalCode());
        }
        if (request.getCountry() != null) {
            details.setCountry(request.getCountry());
        }
        if (request.getPhoneNumber() != null) {
            details.setPhoneNumber(request.getPhoneNumber());
        }
        if (request.getFullName() != null) {
            details.setFullName(request.getFullName());
        }
        // Siempre actualizar el flag isDefault ya que es un booleano primitivo
        details.setDefault(request.isDefault());
    }

    @Override
    public List<ShippingDetails> getShippingDetailsByUserId(Long userId) {
        return shippingDetailsRepository.findByUserIdAndActiveTrue(userId);
    }

    @Override
    public ShippingDetails getDefaultShippingDetails(Long userId) {
        return shippingDetailsRepository.findByUserIdAndIsDefaultTrueAndActiveTrue(userId)
                .orElseGet(() -> shippingDetailsRepository.findByUserIdAndActiveTrue(userId).stream()
                        .findFirst()
                        .orElse(null));
    }

    @Override
    @Transactional
    public ShippingDetails setDefaultShippingAddress(Long userId, Long addressId) {
        // Verificar si la dirección pertenece al usuario
        ShippingDetails addressToMakeDefault = shippingDetailsRepository.findById(addressId)
                .filter(address -> address.getUser().getId().equals(userId) && address.isActive())
                .orElseThrow(() -> new ResourceNotFoundException("Shipping address not found or not owned by this user"));

        // Desactivar todas las direcciones predeterminadas
        setAllShippingDetailsNonDefault(userId);

        // Establecer la nueva dirección predeterminada
        addressToMakeDefault.setDefault(true);
        return shippingDetailsRepository.save(addressToMakeDefault);
    }

    @Override
    @Transactional
    public void deleteShippingAddress(Long userId, Long addressId) {
        ShippingDetails addressToDelete = shippingDetailsRepository.findById(addressId)
                .filter(address -> address.getUser().getId().equals(userId))
                .orElseThrow(() -> new ResourceNotFoundException("Shipping address not found or not owned by this user"));

        addressToDelete.setActive(false);
        shippingDetailsRepository.save(addressToDelete);

        // Si eliminamos la dirección predeterminada, establecer otra como predeterminada
        if (addressToDelete.isDefault()) {
            shippingDetailsRepository.findByUserIdAndActiveTrue(userId).stream()
                    .findFirst()
                    .ifPresent(newDefault -> {
                        newDefault.setDefault(true);
                        shippingDetailsRepository.save(newDefault);
                    });
        }
    }

    @Override
    public ShippingDetails getShippingAddressById(Long userId, Long addressId) {
        return shippingDetailsRepository.findById(addressId)
                .filter(address -> address.getUser().getId().equals(userId) && address.isActive())
                .orElseThrow(() -> new ResourceNotFoundException("Shipping address not found or not owned by this user"));
    }

    private void setAllShippingDetailsNonDefault(Long userId) {
        List<ShippingDetails> allAddresses = shippingDetailsRepository.findByUserIdAndActiveTrue(userId);
        allAddresses.forEach(address -> address.setDefault(false));
        shippingDetailsRepository.saveAll(allAddresses);
    }

}

