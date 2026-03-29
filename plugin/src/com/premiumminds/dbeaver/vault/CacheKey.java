package com.premiumminds.dbeaver.vault;

public record CacheKey(String address, String secret, SecretType secretType) {

}