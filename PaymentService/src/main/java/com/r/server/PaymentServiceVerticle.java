package com.r.server;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.r.core.Account;
import com.r.core.PaymentRequest;
import com.r.core.PaymentServiceConstants;
import com.r.server.http.Endpoint;
import com.r.server.http.JsonMessageConsumer;
import com.r.server.payment.PaymentService;
import com.r.server.payment.TransactionResult;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;

import java.math.BigDecimal;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class PaymentServiceVerticle extends AbstractVerticle implements JsonMessageConsumer {
    private static final Logger LOGGER = Logger.getLogger(PaymentServiceVerticle.class.getCanonicalName());

    public static final String GET_ACCOUNT = "http.get.account";
    public static final String TRANSFER = "http.transfer";

    private final PaymentService paymentService;
    private final int port;

    @Inject
    public PaymentServiceVerticle(PaymentService paymentService, @Named(PaymentServiceConstants.PAYMENT_SERVICE_PORT) int port) {
        this.paymentService = paymentService;
        this.port = port;
    }

    private List<Endpoint> getEndpoints() {
        List<Endpoint> endpoints = new LinkedList<>();
        endpoints.add(new Endpoint(vertx, HttpMethod.GET, "/account/:accountId", GET_ACCOUNT, this::getAccount));
        //endpoints.add(new Endpoint(vertx, HttpMethod.GET, "/transfer/:fromAccountId/:toAccountId/:currency/:quantity", TRANSFER, this::transfer));
        endpoints.add(new Endpoint(vertx, HttpMethod.POST, "/transfer*", TRANSFER, this::transfer));
        return endpoints;
    }

    @Override
    public void start(Future<Void> startFuture) {
        EventBus eventBus = vertx.eventBus();

        Router router = Router.router(vertx);
        router.route().handler(BodyHandler.create());
        for (Endpoint endpoint : getEndpoints()) {
            router.route(endpoint.getHttpMethod(), endpoint.getEndpoint()).handler(endpoint);
            JsonMessageConsumer.registerMessageConsumer(eventBus, endpoint.getEventBusMessageAddress(), endpoint.getHandler());
        }

        vertx.createHttpServer().requestHandler(router::accept).listen(port, result -> {
            if (result.succeeded()) {
                LOGGER.info("Payment Service is up and running.");
                startFuture.complete();
            } else {
                LOGGER.severe("Payment Service fails to start!");
                startFuture.fail(result.cause());
            }
        });
    }


    private JsonObject getAccount(JsonObject message) {
        String accountId = Optional.of(message)
                .map(m -> m.getJsonObject("params"))
                .map(p -> p.getString("accountId"))
                .orElse(null);

        Account account = paymentService.getAccount(accountId);
        if (account == null) {
            throw new RuntimeException("Account not found.");
        }

        Map<String, Object> map = account.getBalances().entrySet().stream().collect(
                Collectors.<Map.Entry<String, BigDecimal>, String, Object>toMap(
                        entry -> entry.getKey(),
                        entry -> entry.getValue().toPlainString()
                )
        );

        JsonObject json = new JsonObject();
        json.put("accountId", account.getAccountId());
        json.put("balance", new JsonObject(map));
        return json;
    }

    private JsonObject transfer(JsonObject message) {
        Optional<JsonObject> parameters = Optional.of(message).map(m -> m.getJsonObject("params"));

        BigDecimal quantity = parameters.map(p -> p.getString("quantity")).map(qty -> new BigDecimal(qty)).orElse(null);

        PaymentRequest request = new PaymentRequest(
                parameters.map(p -> p.getString("fromAccountId")).orElse(null),
                parameters.map(p -> p.getString("toAccountId")).orElse(null),
                parameters.map(p -> p.getString("currency")).orElse(null),
                quantity
        );

        TransactionResult result = paymentService.transfer(request);
        if (result.isSuccess()) {
            JsonObject json = new JsonObject();
            json.put(PaymentServiceConstants.TRANSACTION_ID, result.getTransactionId());
            return json;
        } else {
            throw new RuntimeException(result.getCode().toString());
        }
    }
}
