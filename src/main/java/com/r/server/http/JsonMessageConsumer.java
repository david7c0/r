package com.r.server.http;

import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.MessageConsumer;
import io.vertx.core.json.JsonObject;

import java.util.function.Function;

public interface JsonMessageConsumer {
    static void registerMessageConsumer(EventBus eventBus, String messageId, Function<JsonObject, JsonObject> handler) {
        MessageConsumer<JsonObject> consumer = eventBus.consumer(messageId);
        consumer.handler(message -> {
            try {
                JsonObject body = message.body();
                JsonObject result = handler.apply(body);
                message.reply(result);
            } catch (Throwable e) {
                message.fail(500, e.getMessage());
            }
        });
    }
}
