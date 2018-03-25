package com.r.server;

import com.google.inject.AbstractModule;
import com.google.inject.name.Names;
import com.r.core.PaymentServiceConstants;
import com.r.server.payment.HazelcastPaymentService;
import com.r.server.payment.PaymentService;
import com.r.server.payment.TransactionIdGenerator;

public class PaymentServiceModule extends AbstractModule {
    private final int port;

    // Can pass in a config or properties object, rather than just a port number.
    public PaymentServiceModule(int port) {
        this.port = port;
    }

    @Override
    protected void configure() {
        bind(TransactionIdGenerator.class);
        bind(PaymentService.class).to(HazelcastPaymentService.class);
        bind(PaymentServiceVerticle.class);
        bind(Integer.class).annotatedWith(Names.named(PaymentServiceConstants.PAYMENT_SERVICE_PORT)).toInstance(port);
    }
}
