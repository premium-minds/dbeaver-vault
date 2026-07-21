package com.premiumminds.vault.client;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

public interface VaultClient {

    Credentials getCredentials(String secret, Request credentialsRequest);

    static Builder builder() {
        return new Builder();
    }

    final class Builder {
        private String address;
        private Path certificate;
        private String namespace;
        private VaultTokenLoader vaultTokenLoader;
        private boolean cache;

        private Builder() {
        }

        public Builder address(String address) {
            this.address = address;
            return this;
        }

        public Builder certificate(Path certificate) {
            this.certificate = certificate;
            return this;
        }

        public Builder namespace(String namespace) {
            this.namespace = namespace;
            return this;
        }

        public Builder tokenLoader(VaultTokenLoader vaultTokenLoader) {
            this.vaultTokenLoader = vaultTokenLoader;
            return this;
        }

        public Builder cache(boolean cache) {
            this.cache = cache;
            return this;
        }

        private static Path validateCertificate(Path certificate) {
            if (!Files.exists(certificate)) {
                throw new IllegalArgumentException(
                        "Vault certificate file does not exist: " + certificate);
            }
            if (!Files.isRegularFile(certificate)) {
                throw new IllegalArgumentException(
                        "Vault certificate path is not a file: " + certificate);
            }
            return certificate;
        }

        public VaultClient build() {
            if (this.address == null) {
                throw new IllegalStateException("address is null");
            }
            if (this.vaultTokenLoader == null) {
                throw new IllegalStateException("vaultTokenLoader is null");
            }

            final var certificateOpt =
                    Optional.ofNullable(certificate)
                            .map(Builder::validateCertificate);

            final var vaultClient = new VaultClientImpl(address,
                    certificateOpt,
                    Optional.ofNullable(namespace),
                    vaultTokenLoader);
            if (cache) {
                return new CacheClient(address, vaultClient);
            }
            return vaultClient;
        }
    }
}
