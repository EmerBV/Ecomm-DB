package com.emerbv.ecommdb.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonValue;

public enum OrderStatus {
    PENDING,
    PROCESSING,
    PAID,
    SHIPPED,
    DELIVERED,
    CANCELLED;

    @JsonCreator
    public static OrderStatus fromString(String value) {
        if (value == null) {
            return null;
        }

        for (OrderStatus status : OrderStatus.values()) {
            if (status.name().equalsIgnoreCase(value)) {
                return status;
            }
        }

        throw new IllegalArgumentException("Unknown order status: " + value);
    }

    @JsonValue
    public String getValue() {
        return this.name();
    }
}
