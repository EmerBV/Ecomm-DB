package com.emerbv.ecommdb.util;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Component
public class PayPalUtils {

    /**
     * Formatea el importe para PayPal (2 decimales)
     */
    public BigDecimal formatAmount(BigDecimal amount) {
        return amount.setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Convierte código de país a formato ISO 2 letras
     */
    public String getCountryCode(String country) {
        if (country == null) return "ES";

        switch (country.toLowerCase()) {
            case "españa":
            case "spain":
                return "ES";
            case "estados unidos":
            case "united states":
            case "usa":
                return "US";
            case "méxico":
            case "mexico":
                return "MX";
            default:
                return "ES";
        }
    }

    /**
     * Genera un ID de referencia único para PayPal basado en el ID de la orden
     */
    public String generateReferenceId(Long orderId) {
        return "ORD_" + orderId + "_" + System.currentTimeMillis();
    }
}
