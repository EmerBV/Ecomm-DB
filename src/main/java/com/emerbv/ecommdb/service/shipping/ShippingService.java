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
                return shippingDetailsRepository.findById(request.getId())
                        .filter(details -> details.getUser().getId().equals(user.getId()))
                        .map(existingDetails -> {
                            updateShippingDetailsFields(existingDetails, request);
                            return shippingDetailsRepository.save(existingDetails);
                        })
                        .orElseThrow(() -> new ResourceNotFoundException("Shipping details not found or not owned by this user"));
            }

            // Si es una nueva dirección
            ShippingDetails newDetails = new ShippingDetails();
            newDetails.setUser(user);
            updateShippingDetailsFields(newDetails, request);

            // Si es la primera dirección o está marcada como predeterminada
            if (request.isDefault() || getShippingDetailsByUserId(user.getId()).isEmpty()) {
                // Desactivar cualquier otra dirección predeterminada
                setAllShippingDetailsNonDefault(user.getId());
                newDetails.setDefault(true);
            }

            return shippingDetailsRepository.save(newDetails);
        } catch (Exception e) {
            // Loggear el error
            System.err.println("Error saving shipping details: " + e.getMessage());
            throw e; // Re-lanzar para manejarlo en el controlador
        }
    }

    private void updateShippingDetailsFields(ShippingDetails details, ShippingDetailsRequest request) {
        details.setAddress(request.getAddress());
        details.setCity(request.getCity());
        details.setState(request.getState());
        details.setPostalCode(request.getPostalCode());
        details.setCountry(request.getCountry());
        details.setPhoneNumber(request.getPhoneNumber());
        details.setFullName(request.getFullName());
        details.setDefault(request.isDefault());
    }

    @Override
    public List<ShippingDetails> getShippingDetailsByUserId(Long userId) {
        return shippingDetailsRepository.findByUserId(userId);
    }

    @Override
    public ShippingDetails getDefaultShippingDetails(Long userId) {
        return shippingDetailsRepository.findByUserIdAndIsDefaultTrue(userId)
                .orElseGet(() -> shippingDetailsRepository.findByUserId(userId).stream()
                        .findFirst()
                        .orElse(null));
    }

    @Override
    @Transactional
    public ShippingDetails setDefaultShippingAddress(Long userId, Long addressId) {
        // Verificar si la dirección pertenece al usuario
        ShippingDetails addressToMakeDefault = shippingDetailsRepository.findById(addressId)
                .filter(address -> address.getUser().getId().equals(userId))
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

        shippingDetailsRepository.delete(addressToDelete);

        // Si eliminamos la dirección predeterminada, establecer otra como predeterminada
        if (addressToDelete.isDefault()) {
            shippingDetailsRepository.findByUserId(userId).stream()
                    .findFirst()
                    .ifPresent(newDefault -> {
                        newDefault.setDefault(true);
                        shippingDetailsRepository.save(newDefault);
                    });
        }
    }

    private void setAllShippingDetailsNonDefault(Long userId) {
        List<ShippingDetails> allAddresses = shippingDetailsRepository.findByUserId(userId);
        allAddresses.forEach(address -> address.setDefault(false));
        shippingDetailsRepository.saveAll(allAddresses);
    }

}

