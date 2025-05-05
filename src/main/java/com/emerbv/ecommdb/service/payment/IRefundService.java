package com.emerbv.ecommdb.service.payment;

import com.emerbv.ecommdb.model.Refund;
import com.emerbv.ecommdb.request.RefundRequest;
import com.emerbv.ecommdb.response.RefundResponse;
import com.stripe.exception.StripeException;

import java.util.List;

public interface IRefundService {
    RefundResponse createRefund(RefundRequest request) throws StripeException;

    RefundResponse getRefund(String refundId) throws StripeException;

    List<Refund> getRefundsByOrder(Long orderId);

    void syncRefundStatus(String refundId) throws StripeException;
}
