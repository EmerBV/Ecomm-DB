package com.emerbv.ecommdb.service.payment;

import com.emerbv.ecommdb.enums.DisputeStatus;
import com.emerbv.ecommdb.enums.OrderStatus;
import com.emerbv.ecommdb.exceptions.ResourceNotFoundException;
import com.emerbv.ecommdb.model.Dispute;
import com.emerbv.ecommdb.model.Order;
import com.emerbv.ecommdb.model.PaymentTransaction;
import com.emerbv.ecommdb.repository.DisputeRepository;
import com.emerbv.ecommdb.repository.OrderRepository;
import com.emerbv.ecommdb.repository.PaymentTransactionRepository;
import com.emerbv.ecommdb.request.DisputeEvidenceRequest;
import com.emerbv.ecommdb.response.DisputeResponse;
import com.stripe.exception.StripeException;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DisputeService implements IDisputeService {

    private static final Logger logger = LoggerFactory.getLogger(DisputeService.class);

    private final DisputeRepository disputeRepository;
    private final PaymentTransactionRepository paymentTransactionRepository;
    private final OrderRepository orderRepository;

    @Override
    @Transactional
    public Dispute createOrUpdateDispute(String stripeDisputeId, String paymentIntentId) throws StripeException {
        // Buscar si ya existe la disputa localmente
        Optional<Dispute> existingDispute = disputeRepository.findByStripeDisputeId(stripeDisputeId);

        if (existingDispute.isPresent()) {
            return updateDisputeStatus(existingDispute.get());
        }

        // Buscar la transacción de pago relacionada
        PaymentTransaction transaction = paymentTransactionRepository.findByPaymentIntentId(paymentIntentId)
                .orElseThrow(() -> new ResourceNotFoundException("Transacción de pago no encontrada para: " + paymentIntentId));

        // Obtener la orden
        Order order = transaction.getOrder();

        // Obtener detalles de la disputa desde Stripe
        com.stripe.model.Dispute stripeDispute = com.stripe.model.Dispute.retrieve(stripeDisputeId);

        // Crear la disputa local
        Dispute dispute = new Dispute();
        dispute.setStripeDisputeId(stripeDisputeId);
        dispute.setOrder(order);
        dispute.setPaymentIntentId(paymentIntentId);
        dispute.setAmount(order.getTotalAmount());
        dispute.setReason(stripeDispute.getReason());
        dispute.setStatus(DisputeStatus.valueOf(stripeDispute.getStatus().toUpperCase()));
        dispute.setCreatedAt(LocalDateTime.now());

        // Actualizar el estado de la orden
        order.setOrderStatus(OrderStatus.DISPUTED);
        orderRepository.save(order);

        return disputeRepository.save(dispute);
    }

    @Override
    @Transactional
    public Dispute updateDisputeStatus(Dispute dispute) throws StripeException {
        // Obtener detalles actualizados desde Stripe
        com.stripe.model.Dispute stripeDispute = com.stripe.model.Dispute.retrieve(dispute.getStripeDisputeId());

        // Actualizar el estado
        DisputeStatus newStatus = DisputeStatus.valueOf(stripeDispute.getStatus().toUpperCase());
        if (dispute.getStatus() != newStatus) {
            dispute.setStatus(newStatus);
            dispute.setUpdatedAt(LocalDateTime.now());

            // Actualizar también el estado de la orden según el resultado de la disputa
            updateOrderStatusBasedOnDisputeOutcome(dispute.getOrder(), newStatus);

            return disputeRepository.save(dispute);
        }

        return dispute;
    }

    private void updateOrderStatusBasedOnDisputeOutcome(Order order, DisputeStatus disputeStatus) {
        switch (disputeStatus) {
            case WON:
                // Si ganamos la disputa, restaurar el estado anterior
                order.setOrderStatus(OrderStatus.PAID);
                break;
            case LOST:
                // Si perdimos la disputa, marcar como reembolsado forzosamente
                order.setOrderStatus(OrderStatus.REFUNDED);
                break;
            case WARNING_CLOSED:
            case WARNING_NEEDS_RESPONSE:
                // Estos son advertencias, no modifican el estado de la orden
                break;
            default:
                // Para otros estados (en proceso), mantener como disputada
                order.setOrderStatus(OrderStatus.DISPUTED);
        }

        orderRepository.save(order);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DisputeResponse> getAllDisputes() {
        return disputeRepository.findAll().stream()
                .map(this::mapToDisputeResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public DisputeResponse getDisputeById(Long disputeId) {
        Dispute dispute = disputeRepository.findById(disputeId)
                .orElseThrow(() -> new ResourceNotFoundException("Disputa no encontrada: " + disputeId));

        return mapToDisputeResponse(dispute);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DisputeResponse> getDisputesByOrderId(Long orderId) {
        return disputeRepository.findByOrderOrderId(orderId).stream()
                .map(this::mapToDisputeResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public DisputeResponse submitDisputeEvidence(Long disputeId, DisputeEvidenceRequest request) throws StripeException {
        Dispute dispute = disputeRepository.findById(disputeId)
                .orElseThrow(() -> new ResourceNotFoundException("Disputa no encontrada: " + disputeId));

        // Verificar que la disputa esté en estado que permita enviar evidencia
        if (dispute.getStatus() != DisputeStatus.NEEDS_RESPONSE &&
                dispute.getStatus() != DisputeStatus.WARNING_NEEDS_RESPONSE) {
            throw new IllegalStateException("La disputa no está en un estado que permita enviar evidencia");
        }

        // Preparar evidencia para Stripe
        Map<String, Object> evidenceParams = new HashMap<>();

        // Agregar evidencia según lo proporcionado en la solicitud
        if (request.getProductDescription() != null) {
            evidenceParams.put("product_description", request.getProductDescription());
        }

        if (request.getCustomerEmailAddress() != null) {
            evidenceParams.put("customer_email_address", request.getCustomerEmailAddress());
        }

        if (request.getCustomerPurchaseIp() != null) {
            evidenceParams.put("customer_purchase_ip", request.getCustomerPurchaseIp());
        }

        if (request.getServiceDate() != null) {
            evidenceParams.put("service_date", request.getServiceDate().toString());
        }

        if (request.getShippingDocumentation() != null) {
            evidenceParams.put("shipping_documentation", request.getShippingDocumentation());
        }

        if (request.getShippingTrackingNumber() != null) {
            evidenceParams.put("shipping_tracking_number", request.getShippingTrackingNumber());
        }

        if (request.getBillingAddress() != null) {
            evidenceParams.put("billing_address", request.getBillingAddress());
        }

        if (request.getShippingAddress() != null) {
            evidenceParams.put("shipping_address", request.getShippingAddress());
        }

        if (request.getShippingDate() != null) {
            evidenceParams.put("shipping_date", request.getShippingDate().toString());
        }

        if (request.getShippingCarrier() != null) {
            evidenceParams.put("shipping_carrier", request.getShippingCarrier());
        }

        if (request.getCustomerSignature() != null) {
            evidenceParams.put("customer_signature", request.getCustomerSignature());
        }

        if (request.getCustomerCommunication() != null) {
            evidenceParams.put("customer_communication", request.getCustomerCommunication());
        }

        if (request.getRefundPolicyDisclosure() != null) {
            evidenceParams.put("refund_policy_disclosure", request.getRefundPolicyDisclosure());
        }

        if (request.getRefundRefusalExplanation() != null) {
            evidenceParams.put("refund_refusal_explanation", request.getRefundRefusalExplanation());
        }

        if (request.getCancellationPolicy() != null) {
            evidenceParams.put("cancellation_policy", request.getCancellationPolicy());
        }

        if (request.getCancellationPolicyDisclosure() != null) {
            evidenceParams.put("cancellation_policy_disclosure", request.getCancellationPolicyDisclosure());
        }

        if (request.getUncategorizedText() != null) {
            evidenceParams.put("uncategorized_text", request.getUncategorizedText());
        }

        // Enviar la evidencia a Stripe
        Map<String, Object> params = new HashMap<>();
        params.put("evidence", evidenceParams);

        com.stripe.model.Dispute stripeDispute = com.stripe.model.Dispute.retrieve(dispute.getStripeDisputeId());
        stripeDispute.update(params);

        // Guardar la fecha de envío de evidencia
        dispute.setEvidenceSubmittedAt(LocalDateTime.now());
        dispute = disputeRepository.save(dispute);

        return mapToDisputeResponse(dispute);
    }

    @Override
    @Transactional
    public String uploadDisputeFile(Long disputeId, MultipartFile file, String purpose) throws IOException, StripeException {
        Dispute dispute = disputeRepository.findById(disputeId)
                .orElseThrow(() -> new ResourceNotFoundException("Disputa no encontrada: " + disputeId));

        // Verificar que la disputa esté en estado que permita enviar evidencia
        if (dispute.getStatus() != DisputeStatus.NEEDS_RESPONSE &&
                dispute.getStatus() != DisputeStatus.WARNING_NEEDS_RESPONSE) {
            throw new IllegalStateException("La disputa no está en un estado que permita enviar evidencia");
        }

        // Crear un File en Stripe para usar como evidencia
        Map<String, Object> fileParams = new HashMap<>();
        fileParams.put("purpose", "dispute_evidence");
        fileParams.put("file", file.getBytes());

        com.stripe.model.File stripeFile = com.stripe.model.File.create(fileParams);

        // Guardar la referencia del archivo en la disputa
        if ("receipt".equals(purpose)) {
            dispute.setReceiptFileId(stripeFile.getId());
        } else if ("invoice".equals(purpose)) {
            dispute.setInvoiceFileId(stripeFile.getId());
        } else if ("shipping_documentation".equals(purpose)) {
            dispute.setShippingDocumentationFileId(stripeFile.getId());
        } else if ("service_documentation".equals(purpose)) {
            dispute.setServiceDocumentationFileId(stripeFile.getId());
        } else {
            dispute.setAdditionalFileId(stripeFile.getId());
        }

        disputeRepository.save(dispute);

        return stripeFile.getId();
    }

    private DisputeResponse mapToDisputeResponse(Dispute dispute) {
        DisputeResponse response = new DisputeResponse();
        response.setId(dispute.getId());
        response.setStripeDisputeId(dispute.getStripeDisputeId());
        response.setOrderId(dispute.getOrder().getOrderId());
        response.setPaymentIntentId(dispute.getPaymentIntentId());
        response.setAmount(dispute.getAmount());
        response.setReason(dispute.getReason());
        response.setStatus(dispute.getStatus());
        response.setCreatedAt(dispute.getCreatedAt());
        response.setUpdatedAt(dispute.getUpdatedAt());
        response.setEvidenceSubmittedAt(dispute.getEvidenceSubmittedAt());
        response.setDueBy(dispute.getDueBy());
        response.setReceiptFileId(dispute.getReceiptFileId());
        response.setInvoiceFileId(dispute.getInvoiceFileId());
        response.setShippingDocumentationFileId(dispute.getShippingDocumentationFileId());
        response.setServiceDocumentationFileId(dispute.getServiceDocumentationFileId());
        response.setAdditionalFileId(dispute.getAdditionalFileId());

        return response;
    }
}
