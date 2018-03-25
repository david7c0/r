package com.r.core;

import java.io.Serializable;
import java.math.BigDecimal;

public class PaymentRequest implements Serializable {
    private final String fromAccountId;
    private final String toAccountId;
    private final String currency;
    private final BigDecimal quantity;

    public PaymentRequest(String fromAccountId, String toAccountId, String currency, BigDecimal quantity) {
        this.fromAccountId = fromAccountId;
        this.toAccountId = toAccountId;
        this.currency = currency;
        this.quantity = quantity;
    }

    public String getFromAccountId() {
        return fromAccountId;
    }

    public String getToAccountId() {
        return toAccountId;
    }

    public String getCurrency() {
        return currency;
    }

    public BigDecimal getQuantity() {
        return quantity;
    }

    @Override
    public String toString() {
        return "PaymentRequest{" +
                "fromAccountId='" + fromAccountId + '\'' +
                ", toAccountId='" + toAccountId + '\'' +
                ", currency='" + currency + '\'' +
                ", quantity=" + quantity +
                '}';
    }
}
