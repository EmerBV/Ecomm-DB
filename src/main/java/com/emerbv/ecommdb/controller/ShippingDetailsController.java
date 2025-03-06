package com.emerbv.ecommdb.controller;

import com.emerbv.ecommdb.exceptions.ResourceNotFoundException;
import com.emerbv.ecommdb.model.ShippingDetails;
import com.emerbv.ecommdb.model.User;
import com.emerbv.ecommdb.request.ShippingDetailsRequest;
import com.emerbv.ecommdb.response.ApiResponse;
import com.emerbv.ecommdb.service.shipping.ShippingService;
import com.emerbv.ecommdb.service.user.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import static org.springframework.http.HttpStatus.NOT_FOUND;

@RequiredArgsConstructor
@RestController
@RequestMapping("${api.prefix}/shipping")
public class ShippingDetailsController {
    private final ShippingService shippingService;
    private final UserService userService; // Para obtener el usuario autenticado

    @PostMapping("/update")
    public ResponseEntity<ApiResponse> addOrUpdateShippingDetails(
            @RequestBody ShippingDetailsRequest request,
            @RequestParam Long userId
    ) {
        try {
            User user = userService.getUserById(userId); // Obtener el usuario
            ShippingDetails updatedDetails = shippingService.addOrUpdateShippingDetails(request, user);
            return ResponseEntity.ok(new ApiResponse("Shipping details updated successfully", updatedDetails));
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(NOT_FOUND).body(new ApiResponse(e.getMessage(), null));
        }
    }

    @GetMapping("/{userId}")
    public ResponseEntity<ApiResponse> getShippingDetails(@PathVariable Long userId) {
        ShippingDetails shippingDetails = shippingService.getShippingDetailsByUserId(userId);
        if (shippingDetails == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ApiResponse("No shipping details found", null));
        }
        return ResponseEntity.ok(new ApiResponse("Shipping details retrieved successfully", shippingDetails));
    }
}
