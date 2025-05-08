package com.emerbv.ecommdb.service.payment;

import com.emerbv.ecommdb.model.Order;
import com.emerbv.ecommdb.request.PayPalPaymentRequest;
import com.emerbv.ecommdb.response.PayPalPaymentResponse;
import com.emerbv.ecommdb.response.PayPalCaptureResponse;

public interface IPayPalService {

    /**
     * Crea una orden de PayPal para iniciar el proceso de pago
     * @param request Contiene información para crear la orden de PayPal
     * @return Respuesta con links para aprobar el pago
     */
    PayPalPaymentResponse createPayment(PayPalPaymentRequest request);

    /**
     * Captura el pago después de que el usuario lo ha aprobado
     * @param payPalOrderId ID de la orden de PayPal aprobada
     * @param orderId ID de la orden en nuestro sistema
     * @return Respuesta con detalles de la captura
     */
    PayPalCaptureResponse capturePayment(String payPalOrderId, Long orderId);

    /**
     * Verifica el estado de un pago
     * @param payPalOrderId ID de la orden de PayPal
     * @return Respuesta con el estado actual del pago
     */
    PayPalPaymentResponse getPaymentDetails(String payPalOrderId);

    /**
     * Actualiza los detalles de pago en la orden local
     * @param order Orden local a actualizar
     * @param payPalOrderId ID de la orden de PayPal
     * @param payPalPayerId ID del pagador en PayPal (cuando aplique)
     * @return Orden actualizada
     */
    Order updatePaymentDetails(Order order, String payPalOrderId, String payPalPayerId);
}
