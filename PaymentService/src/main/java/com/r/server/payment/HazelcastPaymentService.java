package com.r.server.payment;

import com.google.inject.Inject;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.TransactionalMap;
import com.hazelcast.transaction.TransactionContext;
import com.hazelcast.transaction.TransactionOptions;
import com.r.core.Account;
import com.r.core.PaymentRequest;

import java.math.BigDecimal;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.r.core.PaymentServiceConstants.ACCOUNT_MAP;

public class HazelcastPaymentService implements PaymentService {
    private static final Logger LOGGER = Logger.getLogger(HazelcastPaymentService.class.getCanonicalName());

    private final HazelcastInstance hazelcast;
    private final TransactionIdGenerator txnIdGenerator;

    @Inject
    public HazelcastPaymentService(HazelcastInstance hazelcast, TransactionIdGenerator txnIdGenerator) {
        this.hazelcast = hazelcast;
        this.txnIdGenerator = txnIdGenerator;
    }

    @Override
    public Account getAccount(String accountId) {
        return (Account) hazelcast.getMap(ACCOUNT_MAP).get(accountId);
    }

    @Override
    public TransactionResult transfer(PaymentRequest request) {
        final long txnId = txnIdGenerator.get();
        final String fromAccountId = request.getFromAccountId();
        final String toAccountId = request.getToAccountId();
        final String currency = request.getCurrency();
        final BigDecimal quantity = request.getQuantity();

        LOGGER.info(String.format("Processing: %s", request));

        try {
            if (fromAccountId == null || toAccountId == null || fromAccountId.equals(toAccountId) || request.getQuantity().compareTo(BigDecimal.ZERO) <= 0) {
                return new TransactionResult(txnId, TransactionResultCode.Invalid_Argument);
            }

            TransactionOptions options = new TransactionOptions().setTransactionType(TransactionOptions.TransactionType.TWO_PHASE);
            TransactionContext context = hazelcast.newTransactionContext(options);
            context.beginTransaction();

            try {
                Account fromAccount;
                Account toAccount;

                TransactionalMap<String, Account> accountMap = context.getMap(ACCOUNT_MAP);
                // Avoid deadlock by always locking the account with smaller account ID first
                if (fromAccountId.compareTo(toAccountId) < 0) {
                    fromAccount = accountMap.getForUpdate(fromAccountId);
                    toAccount = accountMap.getForUpdate(toAccountId);
                } else {
                    toAccount = accountMap.getForUpdate(toAccountId);
                    fromAccount = accountMap.getForUpdate(fromAccountId);
                }

                if (fromAccount.getBalance(currency).compareTo(quantity) < 0) {
                    context.rollbackTransaction();
                    return new TransactionResult(txnId, TransactionResultCode.Insufficient_Fund);
                } else {
                    fromAccount.withdrawOrDeposit(currency, BigDecimal.valueOf(-1).multiply(quantity));
                    toAccount.withdrawOrDeposit(currency, quantity);
                    accountMap.put(fromAccountId, fromAccount);
                    accountMap.put(toAccountId, toAccount);
                    context.commitTransaction();
                    return new TransactionResult(txnId, TransactionResultCode.Succeed);
                }
            } catch (Throwable e) {
                context.rollbackTransaction();
                LOGGER.log(Level.SEVERE,
                        String.format("An error occurred while processing: %s", request),
                        e);
            }
        } catch (Throwable e) {
            LOGGER.log(Level.SEVERE,
                    String.format("An error occurred while processing: %s", request),
                    e);
        }

        return new TransactionResult(txnId, TransactionResultCode.Failed);
    }
}
