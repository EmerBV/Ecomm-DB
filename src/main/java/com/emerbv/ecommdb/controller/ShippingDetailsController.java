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

import java.util.List;

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
            User user = userService.getUserById(userId);
            ShippingDetails updatedDetails = shippingService.addOrUpdateShippingDetails(request, user);
            return ResponseEntity.ok(new ApiResponse("Shipping details updated successfully", updatedDetails));
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(NOT_FOUND).body(new ApiResponse(e.getMessage(), null));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse("An unexpected error occurred", e.getMessage()));
        }
    }

    @GetMapping("/{userId}")
    public ResponseEntity<ApiResponse> getShippingDetails(@PathVariable Long userId) {
        List<ShippingDetails> shippingDetails = shippingService.getShippingDetailsByUserId(userId);
        if (shippingDetails.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ApiResponse("No shipping details found", null));
        }
        return ResponseEntity.ok(new ApiResponse("Shipping details retrieved successfully", shippingDetails));
    }

    @GetMapping("/{userId}/default")
    public ResponseEntity<ApiResponse> getDefaultShippingDetails(@PathVariable Long userId) {
        ShippingDetails defaultDetails = shippingService.getDefaultShippingDetails(userId);
        if (defaultDetails == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ApiResponse("No default shipping details found", null));
        }
        return ResponseEntity.ok(new ApiResponse("Default shipping details retrieved successfully", defaultDetails));
    }

    @PutMapping("/{userId}/address/{addressId}/default")
    public ResponseEntity<ApiResponse> setDefaultShippingAddress(
            @PathVariable Long userId,
            @PathVariable Long addressId
    ) {
        try {
            ShippingDetails defaultAddress = shippingService.setDefaultShippingAddress(userId, addressId);
            return ResponseEntity.ok(new ApiResponse("Default shipping address updated successfully", defaultAddress));
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(NOT_FOUND).body(new ApiResponse(e.getMessage(), null));
        }
    }

    @DeleteMapping("/{userId}/address/{addressId}")
    public ResponseEntity<ApiResponse> deleteShippingAddress(
            @PathVariable Long userId,
            @PathVariable Long addressId
    ) {
        try {
            shippingService.deleteShippingAddress(userId, addressId);
            return ResponseEntity.ok(new ApiResponse("Shipping address deleted successfully", null));
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(NOT_FOUND).body(new ApiResponse(e.getMessage(), null));
        }
    }

    @GetMapping("/{userId}/address/{addressId}")
    public ResponseEntity<ApiResponse> getShippingAddressById(
            @PathVariable Long userId,
            @PathVariable Long addressId
    ) {
        try {
            ShippingDetails shippingDetails = shippingService.getShippingAddressById(userId, addressId);
            return ResponseEntity.ok(new ApiResponse("Shipping address retrieved successfully", shippingDetails));
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(NOT_FOUND).body(new ApiResponse(e.getMessage(), null));
        }
    }
}
