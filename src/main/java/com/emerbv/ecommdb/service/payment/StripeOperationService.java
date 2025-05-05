package com.emerbv.ecommdb.service.payment;

import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import com.stripe.model.Refund;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.retry.RetryCallback;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.function.Supplier;

@Service
@RequiredArgsConstructor
public class StripeOperationService {
    private static final Logger logger = LoggerFactory.getLogger(StripeOperationService.class);

    private final RetryTemplate stripeRetryTemplate;

    /**
     * Ejecuta una operación de Stripe con reintentos automáticos en caso de fallos
     * @param operation La operación a ejecutar
     * @param operationDescription Descripción de la operación para los logs
     * @param <T> El tipo de retorno de la operación
     * @return El resultado de la operación
     * @throws StripeException Si la operación falla después de todos los reintentos
     */
    public <T> T executeWithRetry(Supplier<T> operation, String operationDescription) throws StripeException {
        try {
            return stripeRetryTemplate.execute((RetryCallback<T, StripeException>) context -> {
                if (context.getRetryCount() > 0) {
                    logger.warn("Reintento {} para operación: {}",
                            context.getRetryCount(), operationDescription);
                }
                return operation.get();
            });
        } catch (Exception e) {
            if (e instanceof StripeException) {
                logger.error("Error en operación Stripe después de reintentos: {} - {}",
                        operationDescription, e.getMessage());
                throw (StripeException) e;
            } else {
                logger.error("Error inesperado en operación Stripe: {} - {}",
                        operationDescription, e.getMessage());
                throw new RuntimeException("Error inesperado en operación Stripe", e);
            }
        }
    }

    // Métodos específicos para operaciones comunes de Stripe

    public PaymentIntent createPaymentIntent(Map<String, Object> params,
                                             com.stripe.net.RequestOptions options) throws StripeException {
        return executeWithRetry(
                () -> {
                    try {
                        return PaymentIntent.create(params, options);
                    } catch (StripeException e) {
                        throw new RuntimeException(e);
                    }
                },
                "createPaymentIntent"
        );
    }

    public PaymentIntent retrievePaymentIntent(String paymentIntentId) throws StripeException {
        return executeWithRetry(
                () -> {
                    try {
                        return PaymentIntent.retrieve(paymentIntentId);
                    } catch (StripeException e) {
                        throw new RuntimeException(e);
                    }
                },
                "retrievePaymentIntent: " + paymentIntentId
        );
    }

    public PaymentIntent confirmPaymentIntent(PaymentIntent intent, Map<String, Object> params)
            throws StripeException {
        return executeWithRetry(
                () -> {
                    try {
                        return intent.confirm(params);
                    } catch (StripeException e) {
                        throw new RuntimeException(e);
                    }
                },
                "confirmPaymentIntent: " + intent.getId()
        );
    }

    public PaymentIntent cancelPaymentIntent(PaymentIntent intent, Map<String, Object> params)
            throws StripeException {
        return executeWithRetry(
                () -> {
                    try {
                        return intent.cancel(params);
                    } catch (StripeException e) {
                        throw new RuntimeException(e);
                    }
                },
                "cancelPaymentIntent: " + intent.getId()
        );
    }

    public Refund createRefund(Map<String, Object> params, com.stripe.net.RequestOptions options)
            throws StripeException {
        return executeWithRetry(
                () -> {
                    try {
                        return Refund.create(params, options);
                    } catch (StripeException e) {
                        throw new RuntimeException(e);
                    }
                },
                "createRefund"
        );
    }

    public Refund retrieveRefund(String refundId) throws StripeException {
        return executeWithRetry(
                () -> {
                    try {
                        return Refund.retrieve(refundId);
                    } catch (StripeException e) {
                        throw new RuntimeException(e);
                    }
                },
                "retrieveRefund: " + refundId
        );
    }

    public com.stripe.model.Dispute retrieveDispute(String disputeId) throws StripeException {
        return executeWithRetry(
                () -> {
                    try {
                        return com.stripe.model.Dispute.retrieve(disputeId);
                    } catch (StripeException e) {
                        throw new RuntimeException(e);
                    }
                },
                "retrieveDispute: " + disputeId
        );
    }

    public com.stripe.model.Dispute updateDispute(String disputeId, Map<String, Object> params) throws StripeException {
        return executeWithRetry(
                () -> {
                    try {
                        com.stripe.model.Dispute dispute = com.stripe.model.Dispute.retrieve(disputeId);
                        return dispute.update(params);
                    } catch (StripeException e) {
                        throw new RuntimeException(e);
                    }
                },
                "updateDispute: " + disputeId
        );
    }

    public com.stripe.model.File createFile(Map<String, Object> params) throws StripeException {
        return executeWithRetry(
                () -> {
                    try {
                        return com.stripe.model.File.create(params);
                    } catch (StripeException e) {
                        throw new RuntimeException(e);
                    }
                },
                "createFile"
        );
    }

    // Puedes agregar más métodos específicos según necesidades
}
