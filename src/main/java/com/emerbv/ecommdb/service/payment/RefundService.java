package com.emerbv.ecommdb.service.payment;

import com.emerbv.ecommdb.enums.OrderStatus;
import com.emerbv.ecommdb.enums.RefundStatus;
import com.emerbv.ecommdb.exceptions.ResourceNotFoundException;
import com.emerbv.ecommdb.model.Order;
import com.emerbv.ecommdb.model.Refund;
import com.emerbv.ecommdb.repository.OrderRepository;
import com.emerbv.ecommdb.repository.RefundRepository;
import com.emerbv.ecommdb.request.RefundRequest;
import com.emerbv.ecommdb.response.RefundResponse;
import com.emerbv.ecommdb.util.StripeUtils;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import com.stripe.net.RequestOptions;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class RefundService implements IRefundService {

    private static final Logger logger = LoggerFactory.getLogger(RefundService.class);

    private final OrderRepository orderRepository;
    private final RefundRepository refundRepository;
    private final IdempotencyService idempotencyService;
    private final StripeUtils stripeUtils;
    private final StripeOperationService stripeOperationService;

    @Override
    @Transactional
    public RefundResponse createRefund(RefundRequest request) throws StripeException {
        // Verificar si ya existe un reembolso con la misma clave de idempotencia
        String idempotencyKey = request.getIdempotencyKey();
        if (idempotencyKey == null) {
            idempotencyKey = idempotencyService.generateIdempotencyKey();
        }

        var existingRecord = idempotencyService.findByKey(idempotencyKey, "REFUND_CREATE");
        if (existingRecord.isPresent() && "SUCCESS".equals(existingRecord.get().getStatus())) {
            String refundId = existingRecord.get().getEntityId();
            com.stripe.model.Refund existingRefund = stripeOperationService.retrieveRefund(refundId);

            Refund localRefund = refundRepository.findByStripeRefundId(refundId)
                    .orElseThrow(() -> new ResourceNotFoundException("Reembolso local no encontrado para ID: " + refundId));

            return mapToRefundResponse(existingRefund, localRefund);
        }

        // Obtener la orden
        Order order = orderRepository.findById(request.getOrderId())
                .orElseThrow(() -> new ResourceNotFoundException("Orden no encontrada: " + request.getOrderId()));

        // Verificar que la orden esté pagada
        if (!OrderStatus.PAID.equals(order.getOrderStatus()) &&
                !OrderStatus.SHIPPED.equals(order.getOrderStatus()) &&
                !OrderStatus.DELIVERED.equals(order.getOrderStatus())) {
            throw new IllegalStateException("No se puede reembolsar una orden que no ha sido pagada");
        }

        // Verificar que la orden tenga un PaymentIntent
        if (order.getPaymentIntentId() == null) {
            throw new IllegalStateException("La orden no tiene un PaymentIntent asociado");
        }

        // Obtener el PaymentIntent para validar
        com.stripe.model.PaymentIntent paymentIntent = stripeOperationService.retrievePaymentIntent(order.getPaymentIntentId());
        if (!"succeeded".equals(paymentIntent.getStatus())) {
            throw new IllegalStateException("No se puede reembolsar un pago que no ha sido completado");
        }

        // Preparar los parámetros para el reembolso
        Map<String, Object> params = new HashMap<>();
        params.put("payment_intent", order.getPaymentIntentId());

        // Si es un reembolso parcial, especificar el monto
        BigDecimal refundAmount = request.getAmount();
        if (refundAmount != null && refundAmount.compareTo(BigDecimal.ZERO) > 0) {
            // Convertir el monto a formato de Stripe (centavos)
            Long amountInCents = stripeUtils.convertAmountToStripeFormat(refundAmount);
            params.put("amount", amountInCents);
        }

        // Agregar motivo si está disponible
        if (request.getReason() != null) {
            params.put("reason", request.getReason().toLowerCase());
        }

        // Agregar metadatos
        Map<String, String> metadata = new HashMap<>();
        metadata.put("orderId", order.getOrderId().toString());
        if (request.getDescription() != null) {
            metadata.put("description", request.getDescription());
        }
        params.put("metadata", metadata);

        // Opciones para la idempotencia
        RequestOptions requestOptions = RequestOptions.builder()
                .setIdempotencyKey(idempotencyKey)
                .build();

        try {
            // Crear el reembolso en Stripe con reintentos
            com.stripe.model.Refund stripeRefund = stripeOperationService.createRefund(params, requestOptions);

            // Crear registro local del reembolso
            Refund refund = new Refund();
            refund.setOrder(order);
            refund.setStripeRefundId(stripeRefund.getId());
            refund.setAmount(refundAmount != null ? refundAmount : order.getTotalAmount());
            refund.setReason(request.getReason());
            refund.setDescription(request.getDescription());
            refund.setStatus(RefundStatus.valueOf(stripeRefund.getStatus().toUpperCase()));
            refund.setCreatedAt(LocalDateTime.now());

            Refund savedRefund = refundRepository.save(refund);

            // Si es un reembolso total, actualizar el estado de la orden
            if (refundAmount == null || refundAmount.compareTo(order.getTotalAmount()) >= 0) {
                order.setOrderStatus(OrderStatus.REFUNDED);
                orderRepository.save(order);
            }

            // Registrar la operación exitosa
            idempotencyService.recordOperation(
                    idempotencyKey,
                    "REFUND_CREATE",
                    stripeRefund.getId(),
                    "SUCCESS"
            );

            return mapToRefundResponse(stripeRefund, savedRefund);

        } catch (StripeException e) {
            // Registrar el error
            idempotencyService.recordOperation(
                    idempotencyKey,
                    "REFUND_CREATE",
                    order.getOrderId().toString(),
                    "ERROR"
            );
            logger.error("Error al crear reembolso para la orden {}: {}", request.getOrderId(), e.getMessage());
            throw e;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public RefundResponse getRefund(String refundId) throws StripeException {
        Refund localRefund = refundRepository.findByStripeRefundId(refundId)
                .orElseThrow(() -> new ResourceNotFoundException("Reembolso no encontrado: " + refundId));

        // Usar el servicio con reintentos
        com.stripe.model.Refund stripeRefund = stripeOperationService.retrieveRefund(refundId);

        // Actualizar el estado local si ha cambiado en Stripe
        if (!localRefund.getStatus().name().equalsIgnoreCase(stripeRefund.getStatus())) {
            localRefund.setStatus(RefundStatus.valueOf(stripeRefund.getStatus().toUpperCase()));
            refundRepository.save(localRefund);
        }

        return mapToRefundResponse(stripeRefund, localRefund);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Refund> getRefundsByOrder(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Orden no encontrada: " + orderId));

        return refundRepository.findByOrder(order);
    }

    @Override
    @Transactional
    public void syncRefundStatus(String refundId) throws StripeException {
        Refund localRefund = refundRepository.findByStripeRefundId(refundId)
                .orElseThrow(() -> new ResourceNotFoundException("Reembolso no encontrado: " + refundId));

        com.stripe.model.Refund stripeRefund = com.stripe.model.Refund.retrieve(refundId);

        // Actualizar el estado local
        RefundStatus newStatus = RefundStatus.valueOf(stripeRefund.getStatus().toUpperCase());
        if (localRefund.getStatus() != newStatus) {
            localRefund.setStatus(newStatus);
            localRefund.setUpdatedAt(LocalDateTime.now());

            // Si el reembolso se ha completado, actualizar la orden si es necesario
            if (newStatus == RefundStatus.SUCCEEDED) {
                Order order = localRefund.getOrder();

                // Verificar si este es un reembolso total
                BigDecimal totalRefunded = getTotalRefundedAmount(order.getOrderId());
                if (totalRefunded.compareTo(order.getTotalAmount()) >= 0) {
                    order.setOrderStatus(OrderStatus.REFUNDED);
                    orderRepository.save(order);
                }
            }

            refundRepository.save(localRefund);
            logger.info("Estado del reembolso {} actualizado a {}", refundId, newStatus);
        }
    }

    private BigDecimal getTotalRefundedAmount(Long orderId) {
        return refundRepository.getTotalRefundedAmountByOrderId(orderId);
    }

    private RefundResponse mapToRefundResponse(com.stripe.model.Refund stripeRefund, Refund localRefund) {
        RefundResponse response = new RefundResponse();
        response.setId(localRefund.getId());
        response.setStripeRefundId(stripeRefund.getId());
        response.setOrderId(localRefund.getOrder().getOrderId());
        response.setAmount(localRefund.getAmount());
        response.setStatus(localRefund.getStatus());
        response.setReason(localRefund.getReason());
        response.setDescription(localRefund.getDescription());
        response.setCreatedAt(localRefund.getCreatedAt());
        response.setUpdatedAt(localRefund.getUpdatedAt());

        // Información adicional de Stripe
        response.setCurrency(stripeRefund.getCurrency());
        response.setPaymentIntentId(stripeRefund.getPaymentIntent());

        return response;
    }
}
