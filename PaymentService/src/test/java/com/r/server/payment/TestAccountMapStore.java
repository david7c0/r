package com.r.server.payment;

import com.hazelcast.core.MapStore;
import com.r.core.Account;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class TestAccountMapStore implements MapStore<String, Account> {
    private final Map<String, Account> accounts = new HashMap<>();

    public TestAccountMapStore(int startAccountId, int endAccountId, String currency, BigDecimal balance) {
        for (int i = startAccountId; i <= endAccountId; i++) {
            Account account = new Account(String.valueOf(i));
            account.withdrawOrDeposit(currency, balance);
            accounts.put(account.getAccountId(), account);
        }
    }

    @Override
    public void store(String key, Account value) {

    }

    @Override
    public void storeAll(Map<String, Account> map) {

    }

    @Override
    public void delete(String key) {

    }

    @Override
    public void deleteAll(Collection<String> keys) {

    }

    @Override
    public Account load(String key) {
        return accounts.get(key);
    }

    @Override
    public Map<String, Account> loadAll(Collection<String> keys) {
        return null;
    }

    @Override
    public Iterable<String> loadAllKeys() {
        return null;
    }
}
