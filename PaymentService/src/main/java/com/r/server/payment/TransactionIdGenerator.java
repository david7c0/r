package com.r.server.payment;

import com.google.inject.Inject;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IdGenerator;

public class TransactionIdGenerator {
    private final IdGenerator idGenerator;

    @Inject
    public TransactionIdGenerator(HazelcastInstance hazelcast) {
        idGenerator = hazelcast.getIdGenerator("newTransactionId");
    }

    public long get() {
        return idGenerator.newId();
    }
}
