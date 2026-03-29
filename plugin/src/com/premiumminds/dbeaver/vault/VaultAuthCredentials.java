package com.premiumminds.dbeaver.vault;

import java.nio.file.Path;

import org.jkiss.dbeaver.model.access.DBAAuthCredentials;

public class VaultAuthCredentials implements DBAAuthCredentials  {

    private String secret;
    private String vaultHost;
    private String tokenFile;
    private SecretType secretType;
    private String usernameKey;
    private String passwordKey;
    private Path certificate;

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

    public SecretType getSecretType() {
        return secretType;
    }

    public void setSecretType(String secretType) {
        if (secretType == null) {
            this.secretType = SecretType.DYNAMIC_ROLE;
        } else {
            this.secretType = SecretType.valueOf(secretType);
        }
    }

    public void setSecretType(SecretType secretType) {
        this.secretType = secretType;
    }

    public String getUsernameKey() {
        return usernameKey;
    }

    public void setUsernameKey(String usernameKey) {
        this.usernameKey = usernameKey;
    }

    public String getPasswordKey() {
        return passwordKey;
    }

    public void setPasswordKey(String passwordKey) {
        this.passwordKey = passwordKey;
    }

    public Path getCertificate() {
        return certificate;
    }

    public void setCertificate(String certificate) {
        if (certificate != null && !certificate.isBlank()) {
            this.certificate = Path.of(certificate);
        }
    }

    @Override
    public boolean isComplete() {
        return true;
    }

}
