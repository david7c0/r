package com.r.server.payment;

import com.hazelcast.config.Config;
import com.hazelcast.config.MapConfig;
import com.hazelcast.config.MapStoreConfig;
import com.hazelcast.config.XmlConfigBuilder;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.r.core.PaymentRequest;
import com.r.core.PaymentServiceConstants;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.math.BigDecimal;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;

public class HazelcastPaymentServiceTest {
    private final int MIN_ACCOUNT_ID = 0;
    private final int MAX_ACCOUNT_ID = 10;
    private final String CURRENCY = "GBP";
    private final BigDecimal BALANCE = BigDecimal.valueOf(10000);

    /***** Test Config *****/
    // Make it easy to configure the test
    private final int THREAD_POOL_SIZE = 10;
    // CONCURRENT_TASK_COUNT * MAX_QUANTITY_PER_TASK <= BALANCE
    private final int CONCURRENT_TASK_COUNT = 1000;
    private final int MAX_QUANTITY_PER_TASK = 10;
    // The test should finish in a second or two. 10 seconds is very sufficient.
    private final int TIMEOUT_SECONDS = 10;

    @Mock
    TransactionIdGenerator idGenerator;

    @Spy
    HazelcastInstance hazelcast = initHazelcastInstance();

    @InjectMocks
    HazelcastPaymentService paymentService;

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    public HazelcastInstance initHazelcastInstance() {
        TestAccountMapStore simpleStore = new TestAccountMapStore(MIN_ACCOUNT_ID, MAX_ACCOUNT_ID, CURRENCY, BALANCE);

        MapStoreConfig mapStoreConfig = new MapStoreConfig();
        mapStoreConfig.setImplementation(simpleStore);
        mapStoreConfig.setWriteDelaySeconds(0);

        XmlConfigBuilder configBuilder = new XmlConfigBuilder();
        Config config = configBuilder.build();
        MapConfig mapConfig = config.getMapConfig(PaymentServiceConstants.ACCOUNT_MAP);
        mapConfig.setMapStoreConfig(mapStoreConfig);

        return Hazelcast.newHazelcastInstance(config);
    }

    @Test
    public void testConcurrentPaymentRequests_AllGoThroughAndCancelOut() throws InterruptedException {
        ExecutorService executor = Executors.newFixedThreadPool(THREAD_POOL_SIZE);

        // Let make sure that we always have sufficient fund.
        // All payments will go through, so that we can easily check the balances at the end.
        assert(CONCURRENT_TASK_COUNT * MAX_QUANTITY_PER_TASK <= BALANCE.intValue());

        long startTime = System.currentTimeMillis();

        for (int i = 0; i < CONCURRENT_TASK_COUNT; i++) {
            executor.submit(() -> {
                String accountA = String.valueOf(ThreadLocalRandom.current().nextInt(MIN_ACCOUNT_ID, MAX_ACCOUNT_ID));
                String accountB = String.valueOf(ThreadLocalRandom.current().nextInt(MIN_ACCOUNT_ID, MAX_ACCOUNT_ID));

                BigDecimal quantity = BigDecimal.valueOf(ThreadLocalRandom.current().nextInt(1, MAX_QUANTITY_PER_TASK + 1));

                // A -> B
                PaymentRequest requestOut = new PaymentRequest(accountA, accountB, CURRENCY, quantity);
                // B -> A, so everything is back to square one
                PaymentRequest requestIn = new PaymentRequest(accountB, accountA, CURRENCY, quantity);

                paymentService.transfer(requestOut);
                paymentService.transfer(requestIn);
            });
        }

        System.out.println("Wait for all payments to clear...");

        try {
            // We may have deadlock if it takes too long (or forever) to complete all payments.
            executor.shutdown();
            if (!executor.awaitTermination(TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                throw new RuntimeException("Timeout! Try again with longer timeout, or check if there is deadlock or performance issue.");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Got interrupted!", e);
        }

        long endTime = System.currentTimeMillis();
        System.out.println(String.format("Execution Time: %d milliseconds", endTime - startTime));

        for (int i = MIN_ACCOUNT_ID; i <= MAX_ACCOUNT_ID; i++) {
            assertEquals(BALANCE, paymentService.getAccount(String.valueOf(i)).getBalance(CURRENCY));
        }
    }
}
