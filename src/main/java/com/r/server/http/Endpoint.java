package com.r.server.http;

import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;

import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public class Endpoint implements Handler<RoutingContext> {
    private final Vertx vertx;
    private final HttpMethod httpMethod;
    private final String endpoint;
    private final String evtBusMsgAddress;
    private final Function<JsonObject, JsonObject> handler;

    public Endpoint(Vertx vertx, HttpMethod httpMethod, String endpoint, String evtBusMsgAddress, Function<JsonObject, JsonObject> handler) {
        this.vertx = vertx;
        this.httpMethod = httpMethod;
        this.endpoint = endpoint;
        this.evtBusMsgAddress = evtBusMsgAddress;
        this.handler = handler;
    }

    public HttpMethod getHttpMethod() {
        return httpMethod;
    }

    public String getEndpoint() {
        return endpoint;
    }

    public String getEventBusMessageAddress() {
        return evtBusMsgAddress;
    }

    public Function<JsonObject, JsonObject> getHandler() {
        return handler;
    }

    @Override
    public void handle(RoutingContext context) {
        JsonObject msg = new JsonObject();


        switch (httpMethod) {
            case GET: {
                Map<String, Object> params = context.request().params().entries().stream().collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
                msg.put("params", new JsonObject(params));
                break;
            }
            case POST: {
                msg.put("params", context.getBodyAsJson());
                break;
            }
        }

        vertx.eventBus().send(evtBusMsgAddress, msg, response -> {
            if (response.succeeded()) {
                context.response()
                        .setStatusCode(200)
                        .putHeader("content-type", "application/json; charset=utf-8")
                        .end(Json.encodePrettily(response.result().body()));
            } else {
                context.response()
                        .setStatusCode(500)
                        .end(response.cause().getMessage());
            }
        });
    }
}
