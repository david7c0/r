package com.r.server.payment;

import com.r.core.Account;

import java.math.BigDecimal;

public class ProblematicAccount extends Account {
    public ProblematicAccount(String accountId, String currency, BigDecimal quantity) {
        super(accountId);
        super.withdrawOrDeposit(currency, quantity);
    }

    @Override
    public boolean withdrawOrDeposit(String currency, BigDecimal quantity) {
        throw new RuntimeException("Expected exception from ProblematicAccount.");
    }
}
