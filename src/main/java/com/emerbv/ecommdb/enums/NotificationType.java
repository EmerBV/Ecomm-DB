package com.emerbv.ecommdb.enums;

public enum NotificationType {
    // Notificaciones relacionadas con pedidos
    ORDER_CONFIRMATION,
    ORDER_SHIPPED,
    ORDER_DELIVERED,
    ORDER_CANCELLED,
    ORDER_REFUNDED,

    // Notificaciones relacionadas con pagos
    PAYMENT_CONFIRMATION,
    PAYMENT_FAILED,
    PAYMENT_REFUNDED,

    // Notificaciones de marketing
    CART_ABANDONED,
    PRODUCT_BACK_IN_STOCK,
    PRICE_DROP,
    SPECIAL_OFFER,
    PROMOTIONAL_CAMPAIGN,

    // Notificaciones de cuenta
    WELCOME,
    PASSWORD_RESET,
    ACCOUNT_VERIFICATION,
    ACCOUNT_LOCKED,

    // Notificaciones del sistema
    SYSTEM_ALERT,
    FEEDBACK_REQUEST
}
