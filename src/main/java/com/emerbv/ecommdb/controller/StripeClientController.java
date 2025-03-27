package com.emerbv.ecommdb.controller;

import com.emerbv.ecommdb.dto.StripeClientDto;
import com.emerbv.ecommdb.response.ApiResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("${api.prefix}/stripe-client")
public class StripeClientController {

    @Value("${stripe.public.key}")
    private String stripePublicKey;

    @GetMapping("/config")
    public ResponseEntity<ApiResponse> getStripeClientConfig() {
        StripeClientDto config = new StripeClientDto();
        config.setPublicKey(stripePublicKey);

        // Aquí puedes establecer otros valores por defecto si es necesario

        return ResponseEntity.ok(new ApiResponse("Stripe configuration retrieved successfully", config));
    }
}
