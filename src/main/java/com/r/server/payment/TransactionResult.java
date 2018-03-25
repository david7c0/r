package com.r.server.payment;

public class TransactionResult {
    private final long transactionId;
    private final TransactionResultCode code;

    public TransactionResult(long transactionId, TransactionResultCode code) {
        this.transactionId = transactionId;
        this.code = code;
    }

    public long getTransactionId() {
        return transactionId;
    }

    public TransactionResultCode getCode() {
        return code;
    }

    public boolean isSuccess() {
        return (code == TransactionResultCode.Succeed);
    }
}
