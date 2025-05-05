package com.emerbv.ecommdb.scheduled;

import com.emerbv.ecommdb.model.IdempotencyRecord;
import com.emerbv.ecommdb.repository.IdempotencyRepository;
import com.emerbv.ecommdb.service.payment.PaymentService;
import com.emerbv.ecommdb.service.payment.RefundService;
import com.emerbv.ecommdb.service.payment.StripeOperationService;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
public class StripeOperationRetryTask {
    private static final Logger logger = LoggerFactory.getLogger(StripeOperationRetryTask.class);

    private final IdempotencyRepository idempotencyRepository;
    private final StripeOperationService stripeOperationService;
    private final PaymentService paymentService;
    private final RefundService refundService;

    /**
     * Tarea programada que se ejecuta cada 15 minutos para reintentar operaciones fallidas
     * Solo reintenta operaciones con antigüedad menor a 24 horas
     */
    @Scheduled(fixedRate = 15 * 60 * 1000) // 15 minutos
    @Transactional
    public void retryFailedOperations() {
        logger.info("Iniciando tarea de reintento de operaciones Stripe fallidas");

        // Buscar operaciones fallidas en las últimas 24 horas
        LocalDateTime cutoffTime = LocalDateTime.now().minusHours(24);

        List<IdempotencyRecord> failedOperations = idempotencyRepository.findByStatusAndCreatedAtAfter("ERROR", cutoffTime);

        logger.info("Encontradas {} operaciones fallidas para reintentar", failedOperations.size());

        for (IdempotencyRecord record : failedOperations) {
            try {
                switch (record.getOperationType()) {
                    case "PAYMENT_INTENT_CREATE":
                        retryPaymentIntentCreation(record);
                        break;
                    case "PAYMENT_INTENT_CONFIRM":
                        retryPaymentIntentConfirmation(record);
                        break;
                    case "REFUND_CREATE":
                        retryRefundCreation(record);
                        break;
                    // Pueden añadirse más tipos de operaciones según sea necesario
                    default:
                        logger.warn("Tipo de operación no soportada para reintento: {}", record.getOperationType());
                }
            } catch (Exception e) {
                logger.error("Error al reintentar operación {}: {}",
                        record.getId(), e.getMessage(), e);
            }
        }

        logger.info("Tarea de reintento de operaciones Stripe completada");
    }

    private void retryPaymentIntentCreation(IdempotencyRecord record) {
        logger.info("Reintentando creación de PaymentIntent para operación: {}", record.getId());

        // Aquí recuperaríamos los datos necesarios desde la BD para reintento
        // Por ejemplo, recuperar la orden asociada y recrear el PaymentIntent

        // Por ahora, solo verificamos si el entityId corresponde a un orderId
        try {
            Long orderId = Long.parseLong(record.getEntityId());
            // Podríamos llamar a paymentService para reintentar la creación
            // paymentService.retryCreatePaymentIntent(orderId, record.getKey());

            // Por ahora solo marcamos como RETRIED
            record.setStatus("RETRIED");
            record.setUpdatedAt(LocalDateTime.now());
            idempotencyRepository.save(record);

        } catch (NumberFormatException e) {
            logger.error("ID de entidad no es un orderId válido: {}", record.getEntityId());
        }
    }

    private void retryPaymentIntentConfirmation(IdempotencyRecord record) {
        logger.info("Reintentando confirmación de PaymentIntent: {}", record.getEntityId());

        try {
            // Recuperar el PaymentIntent y verificar su estado actual
            PaymentIntent intent = stripeOperationService.retrievePaymentIntent(record.getEntityId());

            if ("requires_confirmation".equals(intent.getStatus())) {
                // Si aún requiere confirmación, reintentamos
                paymentService.confirmPayment(record.getEntityId());

                // Actualizar el registro
                record.setStatus("SUCCESS");
                record.setUpdatedAt(LocalDateTime.now());
                idempotencyRepository.save(record);

                logger.info("Reintento exitoso para confirmación de PaymentIntent: {}", record.getEntityId());
            } else {
                // Si ya no requiere confirmación (posiblemente ya fue confirmado por otra vía)
                logger.info("PaymentIntent {} ya no requiere confirmación, estado actual: {}",
                        record.getEntityId(), intent.getStatus());

                // Actualizar el registro
                record.setStatus("RESOLVED");
                record.setUpdatedAt(LocalDateTime.now());
                idempotencyRepository.save(record);
            }

        } catch (StripeException e) {
            logger.error("Error al reintentar confirmación de PaymentIntent {}: {}",
                    record.getEntityId(), e.getMessage());

            // Actualizar el número de reintentos y la última fecha de intento
            record.setUpdatedAt(LocalDateTime.now());
            idempotencyRepository.save(record);
        }
    }

    private void retryRefundCreation(IdempotencyRecord record) {
        logger.info("Reintentando creación de Refund para operación: {}", record.getId());

        // Lógica similar a la de reintentar PaymentIntent, pero para reembolsos
        // Por ahora, solo actualizamos el registro para indicar que se reintentó

        record.setStatus("RETRIED");
        record.setUpdatedAt(LocalDateTime.now());
        idempotencyRepository.save(record);
    }
}
