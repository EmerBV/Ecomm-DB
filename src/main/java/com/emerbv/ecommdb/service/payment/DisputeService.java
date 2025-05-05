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
    private final StripeOperationService stripeOperationService;

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

        // Obtener detalles de la disputa desde Stripe con reintentos
        com.stripe.model.Dispute stripeDispute = stripeOperationService.retrieveDispute(stripeDisputeId);

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
        // Obtener detalles actualizados desde Stripe con reintentos
        com.stripe.model.Dispute stripeDispute = stripeOperationService.retrieveDispute(dispute.getStripeDisputeId());

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
        // Este método no necesita cambios porque no interactúa directamente con Stripe
        switch (disputeStatus) {
            case WON:
                order.setOrderStatus(OrderStatus.PAID);
                break;
            case LOST:
                order.setOrderStatus(OrderStatus.REFUNDED);
                break;
            case WARNING_CLOSED:
            case WARNING_NEEDS_RESPONSE:
                break;
            default:
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

        // Añadir los diferentes tipos de evidencia al mapa evidenceParams
        // Este código no cambia, solo se añade para cada campo de request

        // Crear el mapa de parámetros completo
        Map<String, Object> params = new HashMap<>();
        params.put("evidence", evidenceParams);

        // Usar el servicio con reintentos para actualizar la disputa
        stripeOperationService.updateDispute(dispute.getStripeDisputeId(), params);

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

        // Crear el archivo en Stripe con reintentos
        com.stripe.model.File stripeFile = stripeOperationService.createFile(fileParams);

        // Guardar la referencia del archivo en la disputa según el propósito
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
