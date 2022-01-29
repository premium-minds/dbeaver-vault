package com.premiumminds.dbeaver.vault;

import org.jkiss.dbeaver.model.auth.DBAAuthCredentials;

public class VaultAuthCredentials implements DBAAuthCredentials  {

    private String secret;
    private String vaultHost;
    private String tokenFile;

    public String getSecret() {
        return secret;
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }

    public String getVaultHost() {
        return vaultHost;
    }

    public void setVaultHost(String vaultHost) {
        this.vaultHost = vaultHost;
    }

    public String getTokenFile() {
        return tokenFile;
    }

    public void setTokenFile(String tokenFile) {
        this.tokenFile = tokenFile;
    }

    @Override
    public boolean isComplete() {
        return true;
    }

}
