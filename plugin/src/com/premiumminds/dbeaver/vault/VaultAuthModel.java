package com.premiumminds.dbeaver.vault;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
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

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;


public class VaultAuthModel implements DBAAuthModel<VaultAuthCredentials>  {

    private static final ILog log = Platform.getLog(VaultAuthModel.class);
    
    private static final String DEFAULT_VAULT_TOKEN_FILE = ".vault-token";

    public static final String PROP_SECRET = "secret";
    public static final String PROP_ADDRESS = "address";
    public static final String PROP_TOKEN_FILE = "token_file";

    private static final HttpClient httpClient = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_1_1)
            .connectTimeout(Duration.ofSeconds(10))
            .build();

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
    public Object initAuthentication(DBRProgressMonitor monitor, DBPDataSource dataSource, VaultAuthCredentials credentials, DBPConnectionConfiguration configuration, Properties connectProps) throws DBException {

        final var address = credentials.getVaultHost();
        final var secret = credentials.getSecret();

        if (address != null && secret != null) {

            final var uri = URI.create(address).resolve("/v1/").resolve(secret);

            try {

                final var token = getTokenFilePath(credentials);

                final var request = HttpRequest.newBuilder()
                        .GET()
                        .header("X-Vault-Token", token)
                        .uri(uri)
                        .build();

                final var response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() != HttpURLConnection.HTTP_OK) {
                    throw new DBException(response.body());
                } else {
                    final var gson = new GsonBuilder()
                            .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
                            .create();
                    final var secretResponse = gson.fromJson(response.body(), DynamicSecretResponse.class);

                    log.info("Username used " + secretResponse.getData().getUsername());

                    connectProps.put(DBConstants.DATA_SOURCE_PROPERTY_USER, secretResponse.getData().getUsername());
                    connectProps.put(DBConstants.DATA_SOURCE_PROPERTY_PASSWORD, secretResponse.getData().getPassword());
                }
            } catch (IOException | InterruptedException e) {
                throw new DBException(e.getLocalizedMessage(), e);
            }
        }

        return credentials;
    }

    private String getTokenFilePath(VaultAuthCredentials credentials) throws IOException {
        Path path;
        final var tokenFile = credentials.getTokenFile();
        if (tokenFile != null && !tokenFile.isBlank()) {
            path = Paths.get(tokenFile);
        } else {
            path = Paths.get(System.getProperty("user.home"), DEFAULT_VAULT_TOKEN_FILE);
        }
        return Files.readAllLines(path).get(0);
    }

    @Override
    public void endAuthentication(DBPDataSourceContainer dataSource, DBPConnectionConfiguration configuration, Properties connProperties) {

    }

    @Override
    public void refreshCredentials(DBRProgressMonitor monitor, DBPDataSourceContainer dataSource, DBPConnectionConfiguration configuration, VaultAuthCredentials credentials) throws DBException {

    }
}