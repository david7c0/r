package com.r.server.payment;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.r.server.HazelcastModule;
import com.r.server.PaymentServiceModule;
import com.r.server.PaymentServiceVerticle;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import io.vertx.ext.web.client.WebClient;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.math.BigDecimal;
import java.net.ServerSocket;

@RunWith(VertxUnitRunner.class)
public class PaymentServiceIntegrationTest {
    private static final String LOCALHOST = "localhost";
    private static final String ACCOUNT_ID_FIELD = "accountId";
    private static final String BALANCE_FIELD = "balance";
    private static final String CURRENCY = "GBP";
    private static final BigDecimal QUANTITY = BigDecimal.valueOf(10000);

    private Vertx vertx;
    private int port;
    private WebClient webClient;

    @Before
    public void setUp(TestContext context) throws Exception {
        ServerSocket socket = new ServerSocket(0);
        port = socket.getLocalPort();
        socket.close();

        Injector injector = Guice.createInjector(
                new HazelcastModule(new TestAccountMapStore(0, 9, CURRENCY, QUANTITY)),
                new PaymentServiceModule(port)
        );

        DeploymentOptions options = new DeploymentOptions().setConfig(new JsonObject().put("http.port", port));

        vertx = Vertx.vertx();
        vertx.deployVerticle(injector.getInstance(PaymentServiceVerticle.class), options, context.asyncAssertSuccess());

        webClient = WebClient.create(vertx);
    }

    @After
    public void tearDown(TestContext context) {
        vertx.close(context.asyncAssertSuccess());
    }

    private void verifyAccountBalance(TestContext context, String accountId, String currency, BigDecimal expectedBalance) {
        Async async = context.async();

        webClient.get(port, LOCALHOST, "/account/" + accountId).send(response -> {
            context.assertTrue(response.succeeded());
            JsonObject json = response.result().bodyAsJsonObject();
            context.assertEquals(accountId, json.getString(ACCOUNT_ID_FIELD));
            context.assertTrue(expectedBalance.compareTo(new BigDecimal(json.getJsonObject(BALANCE_FIELD).getMap().get(currency).toString())) == 0);
            async.complete();
        });

        async.awaitSuccess();
    }

    @Test
    public void testGetAccount(TestContext context) {
        verifyAccountBalance(context, "0", CURRENCY, QUANTITY);
    }

    private void transferAndVerify(TestContext context, String fromAccountId, String toAccountId, String currency, BigDecimal quantity) {
        transferAndVerify(context, fromAccountId, toAccountId, currency, quantity, null);
    }

    private void transferAndVerify(TestContext context, String fromAccountId, String toAccountId, String currency, BigDecimal quantity, String error) {
        Async async = context.async();

        JsonObject requestBody = new JsonObject()
                .put("fromAccountId", fromAccountId)
                .put("toAccountId", toAccountId)
                .put("currency", currency)
                .put("quantity", quantity.toString());

        webClient.post(port, LOCALHOST, "/transfer")
                .sendJsonObject(requestBody, response -> {
                    context.assertTrue(response.succeeded());

                    if (error == null || error.isEmpty()) {
                        JsonObject json = response.result().bodyAsJsonObject();
                        context.assertNotNull(json.getLong("transaction.id"));
                    } else {
                        context.assertEquals(error, response.result().bodyAsString());
                    }

                    async.complete();
                });

        async.awaitSuccess();
    }

    @Test
    public void testTransfer(TestContext context) {
        final String fromAccountId = "0";
        final String toAccountId = "1";
        final BigDecimal amount = BigDecimal.valueOf(1.234);

        // A -> B
        transferAndVerify(context, fromAccountId, toAccountId, CURRENCY, amount);
        verifyAccountBalance(context, fromAccountId, CURRENCY, QUANTITY.subtract(amount));
        verifyAccountBalance(context, toAccountId, CURRENCY, QUANTITY.add(amount));

        // B -> A
        transferAndVerify(context, toAccountId, fromAccountId, CURRENCY, amount);
        verifyAccountBalance(context, fromAccountId, CURRENCY, QUANTITY);
        verifyAccountBalance(context, toAccountId, CURRENCY, QUANTITY);
    }

    @Test
    public void testTransfer_Insufficient_Fund(TestContext context) {
        final String fromAccountId = "0";
        final String toAccountId = "1";
        final BigDecimal amount = QUANTITY.add(BigDecimal.valueOf(0.01));

        transferAndVerify(context, fromAccountId, toAccountId, CURRENCY, amount, "Insufficient_Fund");

        // Balances should remain the same.
        verifyAccountBalance(context, fromAccountId, CURRENCY, QUANTITY);
        verifyAccountBalance(context, toAccountId, CURRENCY, QUANTITY);
    }
}
