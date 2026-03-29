package com.premiumminds.dbeaver.vault;

import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
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

import com.premiumminds.vault.client.Credentials;
import com.premiumminds.vault.client.DefaultVaultTokenLoader;
import com.premiumminds.vault.client.Lease;
import com.premiumminds.vault.client.Request;
import com.premiumminds.vault.client.VaultClient;

public class VaultAuthModel implements DBAAuthModel<VaultAuthCredentials>  {

    private static final ILog log = Platform.getLog(VaultAuthModel.class);
        
    public static final String PROP_SECRET = "secret";
    public static final String PROP_ADDRESS = "address";
    public static final String PROP_TOKEN_FILE = "token_file";
    private static final String ENV_VAULT_AGENT_ADDR = "VAULT_AGENT_ADDR";
    private static final String ENV_VAULT_ADDR = "VAULT_ADDR";
    private static final String ERROR_VAULT_ADDRESS_NOT_DEFINED = "Vault address not defined";
    private static final String ERROR_VAULT_SECRET_NOT_DEFINED = "Vault secret not defined";

    private static final Map<CacheKey, Credentials> secretsCache = new ConcurrentHashMap<>();

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
        return credentials;
    }

    @Override
    public void saveCredentials(DBPDataSourceContainer dataSource, DBPConnectionConfiguration configuration, VaultAuthCredentials credentials) {
        configuration.setAuthProperty(PROP_SECRET, credentials.getSecret());
        configuration.setAuthProperty(PROP_ADDRESS, credentials.getVaultHost());
        configuration.setAuthProperty(PROP_TOKEN_FILE, credentials.getTokenFile());
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

        log.info("Address used: " + address);
        log.info("Secret used: " + secret);
        
        DefaultVaultTokenLoader vaultTokenLoader = new DefaultVaultTokenLoader(
                Optional.ofNullable(credentials.getTokenFile()).map(Path::of),
                address
        );

        final var credentialsRequest = Request.dynamicRequest();
        final var key = new CacheKey(address, secret);
        final var value = secretsCache.compute(key, (k, v) -> {
            final var vaultClient = VaultClient.builder()
                    .withAddress(address)
                    .withTokenLoader(vaultTokenLoader)
                    .build();
            try {
                if (v == null) {
                    return vaultClient.getCredentials(secret, credentialsRequest);
                } else {
                    if (v instanceof Lease lease) {
                        final var leaseOpt = vaultClient.getLease(lease.leaseId());
                        if (leaseOpt.isEmpty()) {
                            return vaultClient.getCredentials(secret, credentialsRequest);
                        }
                    }
                }
                return v;
            } catch (Exception e) {
                throw new RuntimeException("Problem connecting to Vault: " + e.getMessage(), e);
            }
        });


        log.info("Username used " + value.username());

        connectProps.put(DBConstants.DATA_SOURCE_PROPERTY_USER, value.username());
        connectProps.put(DBConstants.DATA_SOURCE_PROPERTY_PASSWORD, value.password());

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

    @Override
    public void endAuthentication(DBPDataSourceContainer dataSource, DBPConnectionConfiguration configuration, Properties connProperties) {

    }

    @Override
    public void refreshCredentials(DBRProgressMonitor monitor, DBPDataSourceContainer dataSource, DBPConnectionConfiguration configuration, VaultAuthCredentials credentials) throws DBException {

    }
}
