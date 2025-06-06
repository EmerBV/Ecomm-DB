package com.emerbv.ecommdb.util;

import com.emerbv.ecommdb.exceptions.StripeException;
import com.stripe.model.PaymentIntent;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.Map;

@Component
public class StripeUtils {

    /**
     * Convierte un monto de dinero en la unidad más pequeña que Stripe necesita (centavos/peniques)
     * @param amount Importe en unidad principal (por ejemplo, dólares)
     * @return Importe en la unidad más pequeña (centavos)
     */
    public Long convertAmountToStripeFormat(BigDecimal amount) {
        if (amount == null) {
            throw new StripeException("Amount cannot be null");
        }

        return amount.multiply(new BigDecimal("100"))
                .setScale(0, RoundingMode.HALF_UP)
                .longValue();
    }

    /**
     * Convierte un valor de centavos/peniques de Stripe a la unidad principal (dólares, euros, etc.)
     * @param amount Importe en centavos
     * @return Importe en la unidad principal
     */
    public BigDecimal convertAmountFromStripeFormat(Long amount) {
        if (amount == null) {
            return BigDecimal.ZERO;
        }

        return new BigDecimal(amount).divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
    }

    /**
     * Verifica si un pago ha tenido éxito
     * @param intent PaymentIntent de Stripe
     * @return true si el pago tiene un estado "succeeded"
     */
    public boolean isPaymentSuccessful(PaymentIntent intent) {
        return intent != null && "succeeded".equals(intent.getStatus());
    }

    /**
     * Verifica si un pago requiere una acción adicional (como 3D Secure)
     * @param intent PaymentIntent de Stripe
     * @return true si se requiere una acción adicional
     */
    public boolean requiresAction(PaymentIntent intent) {
        return intent != null && "requires_action".equals(intent.getStatus());
    }

    /**
     * Obtiene un mensaje de error amigable basado en el error del PaymentIntent
     * @param intent PaymentIntent con error
     * @return Mensaje de error legible para el usuario
     */
    public String getReadableErrorMessage(PaymentIntent intent) {
        if (intent == null || intent.getLastPaymentError() == null) {
            return "Error desconocido en el procesamiento del pago";
        }

        String errorCode = intent.getLastPaymentError().getCode();

        switch (errorCode) {
            case "card_declined":
                return "La tarjeta fue rechazada. Por favor, verifica los datos o intenta con otra tarjeta.";
            case "expired_card":
                return "La tarjeta ha expirado. Por favor, utiliza otra tarjeta.";
            case "incorrect_cvc":
                return "El código de seguridad (CVC) es incorrecto. Por favor, verifica e intenta de nuevo.";
            case "processing_error":
                return "Ocurrió un error al procesar la tarjeta. Por favor, intenta de nuevo.";
            case "insufficient_funds":
                return "La tarjeta no tiene fondos suficientes. Por favor, intenta con otra tarjeta.";
            default:
                return "Error en el procesamiento del pago: " +
                        (intent.getLastPaymentError().getMessage() != null ?
                                intent.getLastPaymentError().getMessage() : errorCode);
        }
    }

    /**
     * Convierte un PaymentIntent de Stripe a un mapa serializable
     */
    public Map<String, Object> convertPaymentIntentToMap(PaymentIntent intent) {
        Map<String, Object> result = new HashMap<>();

        // Datos básicos
        result.put("id", intent.getId());
        result.put("status", intent.getStatus());
        result.put("amount", convertAmountFromStripeFormat(intent.getAmount()));
        result.put("currency", intent.getCurrency());
        result.put("clientSecret", intent.getClientSecret());
        result.put("paymentMethod", intent.getPaymentMethod());

        // Metadatos si existen
        if (intent.getMetadata() != null && !intent.getMetadata().isEmpty()) {
            result.put("metadata", intent.getMetadata());
        }

        // Información de error si existe
        if (intent.getLastPaymentError() != null) {
            Map<String, Object> error = new HashMap<>();
            error.put("code", intent.getLastPaymentError().getCode());
            error.put("message", intent.getLastPaymentError().getMessage());
            error.put("type", intent.getLastPaymentError().getType());
            result.put("error", error);
        }

        // Información sobre el estado
        result.put("requiresAction", "requires_action".equals(intent.getStatus()));
        result.put("requiresConfirmation", "requires_confirmation".equals(intent.getStatus()));
        result.put("canceled", "canceled".equals(intent.getStatus()));
        result.put("succeeded", "succeeded".equals(intent.getStatus()));

        return result;
    }
}
