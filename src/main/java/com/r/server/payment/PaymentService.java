package com.r.server.payment;

import com.r.core.Account;
import com.r.core.PaymentRequest;

public interface PaymentService {
    Account getAccount(String accountId);
    TransactionResult transfer(PaymentRequest request);
}
