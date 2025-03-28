package com.emerbv.ecommdb.service.shipping;

import com.emerbv.ecommdb.exceptions.AlreadyExistsException;
import com.emerbv.ecommdb.exceptions.ResourceNotFoundException;
import com.emerbv.ecommdb.model.ShippingDetails;
import com.emerbv.ecommdb.model.User;
import com.emerbv.ecommdb.repository.ShippingDetailsRepository;
import com.emerbv.ecommdb.repository.UserRepository;
import com.emerbv.ecommdb.request.ShippingDetailsRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ShippingService implements IShippingService {
    private final UserRepository userRepository;
    private final ShippingDetailsRepository shippingDetailsRepository;

    @Override
    public ShippingDetails addOrUpdateShippingDetails(ShippingDetailsRequest request, User user) {
        return Optional.ofNullable(getShippingDetailsByUserId(user.getId()))
                .map(existingShippingDetails -> {
                    // Si ya existen, los actualizamos
                    existingShippingDetails.setAddress(request.getAddress());
                    existingShippingDetails.setCity(request.getCity());
                    existingShippingDetails.setState(request.getState());
                    existingShippingDetails.setPostalCode(request.getPostalCode());
                    existingShippingDetails.setCountry(request.getCountry());
                    existingShippingDetails.setPhoneNumber(request.getPhoneNumber());
                    existingShippingDetails.setFullName(request.getFullName());
                    return shippingDetailsRepository.save(existingShippingDetails);
                })
                .orElseGet(() -> {
                    // Si no existen, los creamos
                    ShippingDetails newShippingDetails = new ShippingDetails();
                    newShippingDetails.setUser(user);
                    newShippingDetails.setAddress(request.getAddress());
                    newShippingDetails.setCity(request.getCity());
                    newShippingDetails.setState(request.getState());
                    newShippingDetails.setPostalCode(request.getPostalCode());
                    newShippingDetails.setCountry(request.getCountry());
                    newShippingDetails.setPhoneNumber(request.getPhoneNumber());
                    newShippingDetails.setFullName(request.getFullName());
                    return shippingDetailsRepository.save(newShippingDetails);
                });
    }


    @Override
    public ShippingDetails getShippingDetailsByUserId(Long userId) {
        return shippingDetailsRepository.findByUserId(userId);
    }
}
