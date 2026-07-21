package com.premiumminds.vault.client;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

final class CacheClient implements VaultClient {

    record CacheKey(String address, String secret, Request request) {
    }

    private static final Map<CacheKey, Credentials> secretsCache = new ConcurrentHashMap<>();
    private final String address;
    private final VaultClientImpl client;

    CacheClient(String address, VaultClientImpl client) {
        this.address = address;
        this.client = client;
    }

    @Override
    public Credentials getCredentials(String secret, Request credentialsRequest) {

        final var key = new CacheKey(address, secret, credentialsRequest);

        return secretsCache.compute(key, (k, v) -> {
            try {
                if (v == null) {
                    return client.getCredentials(secret, k.request());
                } else {
                    if (v instanceof Lease lease) {
                        final var leaseOpt = client.getLease(lease.leaseId());
                        if (leaseOpt.isEmpty()) {
                            return client.getCredentials(secret, k.request());
                        }
                    }
                }
                return v;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }
}
