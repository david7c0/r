package com.r.core;

import org.junit.Before;
import org.junit.Test;

import java.math.BigDecimal;

import static junit.framework.TestCase.assertNotNull;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class AccountTest {
    private final String ACCOUNT_ID = "1000";
    private Account account;

    @Before
    void setUp() throws Exception {
        account = new Account(ACCOUNT_ID);
    }

    @Test
    public void testBlankAccount() {
        assertEquals(ACCOUNT_ID, account.getAccountId());
        assertNotNull(account.getBalances());
        assertEquals(account.getBalances().size(), 0);
        assertEquals(account.getBalance("USD"), BigDecimal.ZERO);
        assertEquals(account.getBalance("GBP"), BigDecimal.ZERO);
    }

    @Test
    public void testWithdrawOrDepositNonPositive() {
        assertTrue(account.withdrawOrDeposit("USD", BigDecimal.ZERO));
        assertEquals(account.getBalance("USD"), BigDecimal.ZERO);
        assertFalse(account.withdrawOrDeposit("USD", BigDecimal.ONE.negate()));
        assertEquals(account.getBalance("USD"), BigDecimal.ZERO);

        assertEquals(account.getBalance("GBP"), BigDecimal.ZERO);
    }

    @Test
    public void testWithdrawOrDeposit() {
        assertTrue(account.withdrawOrDeposit("USD", BigDecimal.TEN));
        assertEquals(account.getBalance("USD"), BigDecimal.TEN);
        assertTrue(account.withdrawOrDeposit("USD", BigDecimal.TEN.negate()));
        assertEquals(account.getBalance("USD"), BigDecimal.ZERO);

        assertEquals(account.getBalance("GBP"), BigDecimal.ZERO);
    }
}
