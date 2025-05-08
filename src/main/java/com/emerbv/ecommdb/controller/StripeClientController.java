package com.emerbv.ecommdb.controller;

import com.emerbv.ecommdb.dto.StripeClientDto;
import com.emerbv.ecommdb.response.ApiResponse;
import com.emerbv.ecommdb.security.config.StripeConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("${api.prefix}/stripe-client")
@RequiredArgsConstructor
public class StripeClientController {

    @Value("${stripe.public.key}")
    private String stripePublicKey;

    private final StripeConfig stripeConfig;

    @GetMapping("/config")
    public ResponseEntity<ApiResponse> getStripeClientConfig() {
        StripeClientDto config = new StripeClientDto();
        config.setPublicKey(stripePublicKey);

        // Aquí puedes establecer otros valores por defecto si es necesario
        config.setApplePayEnabled(true);
        config.setApplePayMerchantId(stripeConfig.getApplePayMerchantId());
        config.setApplePayCountry("ES"); // Cambia según tu país
        config.setApplePayCurrency("EUR"); // Cambia según tu moneda
        config.setApplePayLabel(stripeConfig.getApplePayMerchantDisplayName());

        return ResponseEntity.ok(new ApiResponse("Stripe configuration retrieved successfully", config));
    }
}
