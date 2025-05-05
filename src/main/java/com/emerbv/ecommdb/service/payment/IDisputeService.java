package com.emerbv.ecommdb.service.payment;

import com.emerbv.ecommdb.model.Dispute;
import com.emerbv.ecommdb.request.DisputeEvidenceRequest;
import com.emerbv.ecommdb.response.DisputeResponse;
import com.stripe.exception.StripeException;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

public interface IDisputeService {

    Dispute createOrUpdateDispute(String stripeDisputeId, String paymentIntentId) throws StripeException;

    Dispute updateDisputeStatus(Dispute dispute) throws StripeException;

    List<DisputeResponse> getAllDisputes();

    DisputeResponse getDisputeById(Long disputeId);

    List<DisputeResponse> getDisputesByOrderId(Long orderId);

    DisputeResponse submitDisputeEvidence(Long disputeId, DisputeEvidenceRequest evidence) throws StripeException;

    String uploadDisputeFile(Long disputeId, MultipartFile file, String purpose) throws IOException, StripeException;
}
