package com.emerbv.ecommdb.service.shipping;

import com.emerbv.ecommdb.model.ShippingDetails;
import com.emerbv.ecommdb.model.User;
import com.emerbv.ecommdb.request.ShippingDetailsRequest;

public interface IShippingService {
    ShippingDetails addOrUpdateShippingDetails(ShippingDetailsRequest request, User user);
    ShippingDetails getShippingDetailsByUserId(Long userId);
}
