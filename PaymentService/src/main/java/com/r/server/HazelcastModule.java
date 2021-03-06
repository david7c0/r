package com.r.server;

import com.google.inject.AbstractModule;
import com.hazelcast.config.Config;
import com.hazelcast.config.MapConfig;
import com.hazelcast.config.MapStoreConfig;
import com.hazelcast.config.XmlConfigBuilder;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.MapStore;
import com.r.core.Account;
import com.r.core.PaymentServiceConstants;

public class HazelcastModule extends AbstractModule {
    private final MapStore<String, Account> mapStore;

    public HazelcastModule(MapStore<String, Account> mapStore) {
        this.mapStore = mapStore;
    }

    private HazelcastInstance initHazelcast() {
        return Hazelcast.newHazelcastInstance(createConfig());
    }

    private Config createConfig() {
        MapStoreConfig mapStoreConfig = new MapStoreConfig();
        mapStoreConfig.setImplementation(mapStore);
        mapStoreConfig.setWriteDelaySeconds(0);

        XmlConfigBuilder configBuilder = new XmlConfigBuilder();
        Config config = configBuilder.build();
        MapConfig mapConfig = config.getMapConfig(PaymentServiceConstants.ACCOUNT_MAP);
        mapConfig.setMapStoreConfig(mapStoreConfig);

        return config;
    }

    @Override
    protected void configure() {
        HazelcastInstance hazelcast = initHazelcast();
        bind(HazelcastInstance.class).toInstance(hazelcast);
    }
}
