package com.premiumminds.vault.client;

public sealed interface Request permits Request.StaticRequest, Request.DynamicRequest, Request.KV1Request, Request.KV2Request {

    static Request staticRequest() {
        return new StaticRequest();
    }
    static Request dynamicRequest() {
        return new DynamicRequest();
    }
    static Request kv1Request(String userKey, String passKey) {
        return new KV1Request(userKey, passKey);
    }
    static Request kv2Request(String userKey, String passKey) {
        return new KV2Request(userKey, passKey);
    }

    record DynamicRequest() implements Request {
    }

    record StaticRequest() implements Request {
    }

    record KV1Request(String userKey, String passKey) implements Request {
    }

    record KV2Request(String userKey, String passKey) implements Request {
    }

}

