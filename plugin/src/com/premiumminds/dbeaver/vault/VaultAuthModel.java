package com.premiumminds.dbeaver.vault;

import java.nio.file.Path;
import java.util.Optional;
import java.util.Properties;
import org.eclipse.core.runtime.ILog;
import org.eclipse.core.runtime.Platform;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBConstants;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.access.DBAAuthModel;
import org.jkiss.dbeaver.model.connection.DBPConnectionConfiguration;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;

import com.premiumminds.vault.client.DefaultVaultTokenLoader;
import com.premiumminds.vault.client.Request;
import com.premiumminds.vault.client.VaultClient;

public class VaultAuthModel implements DBAAuthModel<VaultAuthCredentials>  {

    private static final ILog log = Platform.getLog(VaultAuthModel.class);

    public static final String PROP_SECRET = "secret";
    public static final String PROP_ADDRESS = "address";
    public static final String PROP_TOKEN_FILE = "token_file";
    public static final String PROP_CERTIFICATE = "certificate";
    public static final String PROP_NAMESPACE = "namespace";
    public static final String PROP_SECRET_TYPE = "secret_type";
    public static final String PROP_USERNAME_KEY = "username_key";
    public static final String PROP_PASSWORD_KEY = "password_key";
    private static final String ENV_VAULT_AGENT_ADDR = "VAULT_AGENT_ADDR";
    private static final String ENV_VAULT_ADDR = "VAULT_ADDR";
    private static final String ENV_VAULT_CACERT = "VAULT_CACERT";
    private static final String ENV_VAULT_NAMESPACE = "VAULT_NAMESPACE";
    private static final String ERROR_VAULT_ADDRESS_NOT_DEFINED = "Vault address not defined";
    private static final String ERROR_VAULT_SECRET_NOT_DEFINED = "Vault secret not defined";

    @NotNull
    public VaultAuthCredentials createCredentials() {
        return new VaultAuthCredentials();
    }

    @Override
    public VaultAuthCredentials loadCredentials(DBPDataSourceContainer dataSource, DBPConnectionConfiguration configuration) {
        VaultAuthCredentials credentials = createCredentials();
        credentials.setSecret(configuration.getAuthProperty(PROP_SECRET));
        credentials.setVaultHost(configuration.getAuthProperty(PROP_ADDRESS));
        credentials.setTokenFile(configuration.getAuthProperty(PROP_TOKEN_FILE));
        credentials.setCertificate(configuration.getAuthProperty(PROP_CERTIFICATE));
        credentials.setSecretType(configuration.getAuthProperty(PROP_SECRET_TYPE));
        credentials.setUsernameKey(configuration.getAuthProperty(PROP_USERNAME_KEY));
        credentials.setPasswordKey(configuration.getAuthProperty(PROP_PASSWORD_KEY));
        return credentials;
    }

    @Override
    public void saveCredentials(DBPDataSourceContainer dataSource, DBPConnectionConfiguration configuration, VaultAuthCredentials credentials) {
        configuration.setAuthProperty(PROP_SECRET, credentials.getSecret());
        configuration.setAuthProperty(PROP_ADDRESS, credentials.getVaultHost());
        configuration.setAuthProperty(PROP_TOKEN_FILE, credentials.getTokenFile());
        configuration.setAuthProperty(PROP_CERTIFICATE, credentials.getCertificate().toString());
        configuration.setAuthProperty(PROP_SECRET_TYPE, credentials.getSecretType().name());
        configuration.setAuthProperty(PROP_USERNAME_KEY, credentials.getUsernameKey());
        configuration.setAuthProperty(PROP_PASSWORD_KEY, credentials.getPasswordKey());
    }

    @Override
    public Object initAuthentication(
            DBRProgressMonitor monitor,
            DBPDataSource dataSource,
            VaultAuthCredentials credentials,
            DBPConnectionConfiguration configuration,
            Properties connectProps) throws DBException
    {

        final var address = getAddress(credentials);
        final var secret = getSecret(credentials);
        final var certificate = getCertificate(credentials);
        final var namespace = getNamespace(credentials);

        DefaultVaultTokenLoader vaultTokenLoader = new DefaultVaultTokenLoader(
                Optional.ofNullable(credentials.getTokenFile()).map(Path::of),
                address
        );

        final Request credentialsRequest = switch (credentials.getSecretType()) {
            case DYNAMIC_ROLE -> Request.dynamicRequest();
            case STATIC_ROLE -> Request.staticRequest();
            case KV1 -> Request.kv1Request(credentials.getUsernameKey(), credentials.getPasswordKey());
            case KV2 -> Request.kv2Request(credentials.getUsernameKey(), credentials.getPasswordKey());
        };

        final var vaultClient = VaultClient.builder()
                .address(address)
                .tokenLoader(vaultTokenLoader)
                .certificate(certificate)
                .namespace(namespace)
                .cache(true)
                .build();
        final var creds = vaultClient.getCredentials(secret, credentialsRequest);

        if (creds.username() == null || creds.password() == null) {
            throw new DBException("There is something wrong with the credentials obtained from Vault");
        }

        log.info("Username used " + creds.username());

        connectProps.put(DBConstants.DATA_SOURCE_PROPERTY_USER, creds.username());
        connectProps.put(DBConstants.DATA_SOURCE_PROPERTY_PASSWORD, creds.password());

        return credentials;
    }

    private String getAddress(VaultAuthCredentials credentials) {
        final var definedAddress = credentials.getVaultHost();
        if (definedAddress != null && !definedAddress.isBlank()) {
            return definedAddress;
        } else {
            final String vaultAgentAddrEnv = System.getenv(ENV_VAULT_AGENT_ADDR);
            if (vaultAgentAddrEnv != null && !vaultAgentAddrEnv.isBlank()){
                return vaultAgentAddrEnv;
            }
            final String vaultAddrEnv = System.getenv(ENV_VAULT_ADDR);
            if (vaultAddrEnv != null && !vaultAddrEnv.isBlank()){
                return vaultAddrEnv;
            }
        }
        throw new RuntimeException(ERROR_VAULT_ADDRESS_NOT_DEFINED);
    }

    private String getSecret(VaultAuthCredentials credentials) {
        final var secret = credentials.getSecret();
        if (secret != null && !secret.isBlank()) {
            return secret;
        }
        throw new RuntimeException(ERROR_VAULT_SECRET_NOT_DEFINED);
    }

    private Path getCertificate(VaultAuthCredentials credentials) {
        final var definedCertificate = credentials.getCertificate();
        if (definedCertificate != null) {
            return definedCertificate;
        } else {
            final String vaultCertificateEnv = System.getenv(ENV_VAULT_CACERT);
            if (vaultCertificateEnv != null && !vaultCertificateEnv.isBlank()){
                return Path.of(vaultCertificateEnv);
            }
        }
        return null;
    }

    private String getNamespace(VaultAuthCredentials credentials) {
        final var definedNamespace = credentials.getNamespace();
        if (definedNamespace != null) {
            return definedNamespace;
        } else {
            final String vaultNamespaceEnv = System.getenv(ENV_VAULT_NAMESPACE);
            if (vaultNamespaceEnv != null && !vaultNamespaceEnv.isBlank()){
                return vaultNamespaceEnv;
            }
        }
        return null;
    }

    @Override
    public void endAuthentication(DBPDataSourceContainer dataSource, DBPConnectionConfiguration configuration, Properties connProperties) {

    }

    @Override
    public void refreshCredentials(DBRProgressMonitor monitor, DBPDataSourceContainer dataSource, DBPConnectionConfiguration configuration, VaultAuthCredentials credentials) throws DBException {

    }
}
