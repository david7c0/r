package com.r.core;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class Account implements Serializable {
    private final String accountId;
    private final Map<String, BigDecimal> balances = new HashMap<>();

    public Account(String accountId) {
        this.accountId = accountId;
    }

    public String getAccountId() {
        return accountId;
    }

    public Map<String, BigDecimal> getBalances() {
        return Collections.unmodifiableMap(balances);
    }

    public BigDecimal getBalance(String currency) {
        return balances.getOrDefault(currency, BigDecimal.ZERO);
    }

    public boolean withdrawOrDeposit(String currency, BigDecimal quantity) {
        BigDecimal newValue = getBalance(currency).add(quantity);
        if (newValue.compareTo(BigDecimal.ZERO) >= 0) {
            balances.put(currency, newValue);
            return true;
        }
        return false;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Account account = (Account) o;
        return Objects.equals(accountId, account.accountId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(accountId);
    }
}
