package com.emerbv.ecommdb.security.config;

import com.stripe.Stripe;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
@Getter
public class StripeConfig {

    @Value("${stripe.secret.key}")
    private String stripeSecretKey;

    @Value("${stripe.apple-pay.merchant-id}")
    private String applePayMerchantId;

    @Value("${stripe.apple-pay.merchant-domain}")
    private String applePayMerchantDomain;

    @Value("${stripe.apple-pay.merchant-display-name}")
    private String applePayMerchantDisplayName;

    @Value("${stripe.apple-pay.certificate-path}")
    private String applePayCertificatePath;

    @Value("${stripe.apple-pay.certificate-password}")
    private String applePayCertificatePassword;

    @PostConstruct
    public void initStripe() {
        Stripe.apiKey = stripeSecretKey;
    }
}
