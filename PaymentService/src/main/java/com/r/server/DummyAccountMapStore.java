package com.r.server;

import com.hazelcast.core.MapStore;
import com.r.core.Account;

import java.math.BigDecimal;
import java.util.*;

public class DummyAccountMapStore implements MapStore<String, Account> {
    private final Map<String, Account> accounts = new HashMap<>();

    public DummyAccountMapStore() {
        for (int i = 0; i <= 10000; i += 1000) {
            Account account = new Account(String.valueOf(i));
            account.withdrawOrDeposit("GBP", BigDecimal.valueOf(i));
            account.withdrawOrDeposit("USD", BigDecimal.valueOf(i));

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
