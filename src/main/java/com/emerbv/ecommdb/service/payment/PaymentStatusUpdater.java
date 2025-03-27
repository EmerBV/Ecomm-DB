package com.emerbv.ecommdb.service.payment;

import com.emerbv.ecommdb.enums.OrderStatus;
import com.emerbv.ecommdb.model.PaymentTransaction;
import com.emerbv.ecommdb.repository.OrderRepository;
import com.emerbv.ecommdb.repository.PaymentTransactionRepository;
import com.stripe.Stripe;
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

/**
 * Componente para sincronizar el estado de los pagos con Stripe
 */
@Component
@RequiredArgsConstructor
public class PaymentStatusUpdater {
    private static final Logger logger = LoggerFactory.getLogger(PaymentStatusUpdater.class);

    private final PaymentTransactionRepository transactionRepository;
    private final OrderRepository orderRepository;

    /**
     * Actualiza periódicamente el estado de las transacciones de pago pendientes
     * Se ejecuta cada 30 minutos
     */
    @Scheduled(fixedRate = 30 * 60 * 1000) // 30 minutos
    @Transactional
    public void updatePendingPayments() {
        logger.info("Starting scheduled payment status update...");

        // Obtener todas las transacciones con estados que no son finales
        List<PaymentTransaction> pendingTransactions = transactionRepository.findAll().stream()
                .filter(transaction ->
                        !"succeeded".equals(transaction.getStatus()) &&
                                !"canceled".equals(transaction.getStatus()) &&
                                !"failed".equals(transaction.getStatus()) &&
                                transaction.getCreatedAt().isAfter(LocalDateTime.now().minusDays(7)) // Solo de los últimos 7 días
                )
                .toList();

        logger.info("Found {} pending transactions to check", pendingTransactions.size());

        for (PaymentTransaction transaction : pendingTransactions) {
            try {
                PaymentIntent intent = PaymentIntent.retrieve(transaction.getPaymentIntentId());

                if (!transaction.getStatus().equals(intent.getStatus())) {
                    updateTransactionAndOrder(transaction, intent);
                }
            } catch (StripeException e) {
                logger.error("Error retrieving payment intent {}: {}",
                        transaction.getPaymentIntentId(), e.getMessage());
            }
        }

        logger.info("Completed scheduled payment status update");
    }

    /**
     * Actualiza una transacción y la orden relacionada con el estado actual de Stripe
     */
    private void updateTransactionAndOrder(PaymentTransaction transaction, PaymentIntent intent) {
        String newStatus = intent.getStatus();
        transaction.setStatus(newStatus);

        if (intent.getLastPaymentError() != null) {
            transaction.setErrorMessage(intent.getLastPaymentError().getMessage());
        }

        if ("succeeded".equals(newStatus)) {
            // Si el pago fue exitoso, actualizar la orden a procesando
            transaction.getOrder().setOrderStatus(OrderStatus.PROCESSING);
            logger.info("Updated order {} to PROCESSING based on succeeded payment",
                    transaction.getOrder().getOrderId());
        } else if ("canceled".equals(newStatus)) {
            // Si el pago fue cancelado, cancelar la orden
            transaction.getOrder().setOrderStatus(OrderStatus.CANCELLED);
            logger.info("Updated order {} to CANCELLED based on canceled payment",
                    transaction.getOrder().getOrderId());
        }

        transactionRepository.save(transaction);
        orderRepository.save(transaction.getOrder());

        logger.info("Updated transaction {} status from {} to {}",
                transaction.getId(), transaction.getStatus(), newStatus);
    }
}
