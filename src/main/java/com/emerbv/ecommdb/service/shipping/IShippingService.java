package com.emerbv.ecommdb.service.shipping;

import com.emerbv.ecommdb.model.ShippingDetails;
import com.emerbv.ecommdb.model.User;
import com.emerbv.ecommdb.request.ShippingDetailsRequest;

import java.util.List;

public interface IShippingService {
    ShippingDetails addOrUpdateShippingDetails(ShippingDetailsRequest request, User user);
    List<ShippingDetails> getShippingDetailsByUserId(Long userId);
    ShippingDetails getDefaultShippingDetails(Long userId);
    ShippingDetails setDefaultShippingAddress(Long userId, Long addressId);
    void deleteShippingAddress(Long userId, Long addressId);
}
