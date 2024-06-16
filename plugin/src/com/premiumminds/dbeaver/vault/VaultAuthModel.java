package com.premiumminds.dbeaver.vault;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

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

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;


public class VaultAuthModel implements DBAAuthModel<VaultAuthCredentials>  {

    private static final ILog log = Platform.getLog(VaultAuthModel.class);
        
    public static final String PROP_SECRET = "secret";
    public static final String PROP_ADDRESS = "address";
    public static final String PROP_TOKEN_FILE = "token_file";
    private static final String ENV_VAULT_AGENT_ADDR = "VAULT_AGENT_ADDR";
    private static final String ENV_VAULT_ADDR = "VAULT_ADDR";
    private static final String ENV_VAULT_CONFIG_PATH = "VAULT_CONFIG_PATH";
    private static final String DEFAULT_VAULT_CONFIG_FILE = ".vault";
    private static final String DEFAULT_VAULT_TOKEN_FILE = ".vault-token";
    private static final String ERROR_VAULT_ADDRESS_NOT_DEFINED = "Vault address not defined";
    private static final String ERROR_VAULT_SECRET_NOT_DEFINED = "Vault secret not defined";
    private static final String ERROR_VAULT_TOKEN_NOT_DEFINED = "Vault token not defined";
    
    private static final HttpClient httpClient = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_1_1)
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    private static final Map<DynamicSecretKey, DynamicSecretResponse> secretsCache = new ConcurrentHashMap<>();

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

        try {

            final var address = getAddress(credentials);
            final var secret = getSecret(credentials);

            log.info("Address used: " + address);
            log.info("Secret used: " + secret);

            DynamicSecretKey key = new DynamicSecretKey(address, secret);
            DynamicSecretResponse value = secretsCache.get(key);

            if (value == null) {

                final var response = getCredentialsFromVault(credentials, address, secret);

                value = response;
                secretsCache.put(key, value);

            } else {
                final var lease = getLeaseFromVault(credentials, address, value.getLeaseId());

                if (!lease.isPresent()) {

                    final var response = getCredentialsFromVault(credentials, address, secret);

                    value = response;
                    secretsCache.put(key, value);
                }
            }

            log.info("Username used " + value.getData().getUsername());

            connectProps.put(DBConstants.DATA_SOURCE_PROPERTY_USER, value.getData().getUsername());
            connectProps.put(DBConstants.DATA_SOURCE_PROPERTY_PASSWORD, value.getData().getPassword());

        } catch (IOException | InterruptedException e) {
            throw new DBException("Problem connecting to Vault: " + e.getMessage(), e);
        }

        return credentials;
    }

	private Optional<LeaseResponse> getLeaseFromVault(
			VaultAuthCredentials credentials,
			final String address,
			final String leaseId)
			throws IOException, InterruptedException, DBException
	{
		final var token = getToken(credentials, address);

		final var uri = URI.create(address).resolve("/v1/sys/leases/lookup");

		final var gson = new GsonBuilder()
		        .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
		        .create();

		final var leaseRequest = new LeaseRequest();
		leaseRequest.setLeaseId(leaseId);

		final var request = HttpRequest.newBuilder()
				.POST(HttpRequest.BodyPublishers.ofString(gson.toJson(leaseRequest)))
		        .header("X-Vault-Token", token)
		        .uri(uri)
		        .build();

		final var response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

		if (response.statusCode() != HttpURLConnection.HTTP_OK) {
			log.info("No lease found for " + leaseId);
		    return Optional.empty();
		}

		return Optional.of(gson.fromJson(response.body(), LeaseResponse.class));
	}

	private DynamicSecretResponse getCredentialsFromVault(
			VaultAuthCredentials credentials,
			final String address,
			final String secret)
			throws IOException, InterruptedException, DBException
	{
		final var token = getToken(credentials, address);

		final var uri = URI.create(address).resolve("/v1/").resolve(secret);

		final var request = HttpRequest.newBuilder()
		        .GET()
		        .header("X-Vault-Token", token)
		        .uri(uri)
		        .build();

		final var response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

		if (response.statusCode() != HttpURLConnection.HTTP_OK) {
		    throw new DBException("Problem connecting to Vault: " + response.body());
		}
		final var gson = new GsonBuilder()
		        .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
		        .create();

		return gson.fromJson(response.body(), DynamicSecretResponse.class);
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
	
    private String getToken(VaultAuthCredentials credentials, String vaultAddress) throws IOException, InterruptedException {

        final var tokenFile = credentials.getTokenFile();
        if (tokenFile != null && !tokenFile.isBlank()) {
            final var path = Paths.get(tokenFile);
            if (path.toFile().exists()){
                return Files.readString(path);
            }
        }

        final var vaultConfigFile = getConfigFile();
        if (vaultConfigFile.toFile().exists()){
            final String token = getTokenFromVaultTokenHelper(vaultConfigFile, vaultAddress);
            if (token != null){
                return token;
            }
        }
        final var defaultTokenFilePath = Paths.get(System.getProperty("user.home"), DEFAULT_VAULT_TOKEN_FILE);
        if (defaultTokenFilePath.toFile().exists()){
            return Files.readString(defaultTokenFilePath);
        }

        throw new RuntimeException(ERROR_VAULT_TOKEN_NOT_DEFINED);
    }
    
    private Path getConfigFile(){
        Path vaultConfigPath = Paths.get(System.getProperty("user.home"), DEFAULT_VAULT_CONFIG_FILE) ;

        final String vaultConfigPathEnv = System.getenv(ENV_VAULT_CONFIG_PATH);
        if (vaultConfigPathEnv != null && !vaultConfigPathEnv.isBlank()){
            vaultConfigPath = Paths.get(vaultConfigPathEnv );
        }

        return vaultConfigPath;
    }
    
    private String getTokenFromVaultTokenHelper(Path configFile, String vaultAddress)
            throws IOException, InterruptedException
    {
    	final Gson gson = new Gson();
    	try(final var fileReader = new FileReader(configFile.toFile())){
    		
    		final VaultConfig config = gson.fromJson(fileReader, VaultConfig.class);
        	
            if (config.tokenHelper != null && !config.tokenHelper.isBlank()){
                final ProcessBuilder processBuilder = new ProcessBuilder();
                processBuilder.environment().putIfAbsent(ENV_VAULT_ADDR, vaultAddress);
                final Process process = processBuilder
                        .command(config.tokenHelper, "get")
                        .start();

                final StreamGobbler streamGobblerErr = new StreamGobbler(process.getErrorStream());
                final StreamGobbler streamGobblerOut = new StreamGobbler(process.getInputStream());

                streamGobblerErr.start();
                streamGobblerOut.start();

                if (!process.waitFor(10, TimeUnit.SECONDS)){
                    throw new RuntimeException("Failure running Vault Token Helper: " + config.tokenHelper + ", took too long to respond.");
                }

                streamGobblerOut.join();
                streamGobblerErr.join();

                if (streamGobblerErr.output != null && !streamGobblerErr.output.isBlank()){
                    throw new RuntimeException("Failure running Vault Token Helper: " + config.tokenHelper + ": " + streamGobblerErr.output);
                }
                return streamGobblerOut.output;
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

    
    private static class StreamGobbler extends Thread {

        private final InputStream stream;

        private String output;

        StreamGobbler(final InputStream stream) {
            this.stream = stream;
        }

        @Override
        public void run() {

            try (BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(stream))) {
                output = bufferedReader.lines().collect(Collectors.joining());
            } catch (IOException e) {
                throw new RuntimeException("Problem reading from Vault Token Helper: " + e.getMessage(), e);
            }
        }
    }
}
