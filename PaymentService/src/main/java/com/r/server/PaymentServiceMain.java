package com.r.server;

import com.google.inject.Guice;
import com.google.inject.Injector;
import io.vertx.core.Vertx;

public class PaymentServiceMain {
    public static int getPort(String[] args) {
        try {
            if (args.length == 1) {
                return Integer.valueOf(args[0]);
            }
        } catch (Throwable e) {
        }
        return 8080;
    }

    public static void main(String[] args) {
        // Can pass in a config or properties object, rather than just a port number.
        Injector injector = Guice.createInjector(
                new HazelcastModule(new DummyAccountMapStore()),
                new PaymentServiceModule(getPort(args))
        );

        Vertx vertx = Vertx.vertx();
        vertx.deployVerticle(injector.getInstance(PaymentServiceVerticle.class));
    }
}
