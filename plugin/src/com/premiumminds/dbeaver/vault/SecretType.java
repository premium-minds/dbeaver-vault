package com.premiumminds.dbeaver.vault;

public enum SecretType {

    DYNAMIC_ROLE("Dynamic role"),
    STATIC_ROLE("Static role"),
    KV1("KV version 1"),
    KV2("KV version 2");

    private final String text;

    SecretType(String text){
        this.text = text;
    }

    public String getText() {
        return text;
    }
}
