package com.premiumminds.dbeaver.vault;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBConstants;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.auth.DBAAuthModel;
import org.jkiss.dbeaver.model.connection.DBPConnectionConfiguration;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;

import com.google.gson.Gson;
import com.google.gson.stream.JsonReader;


public class VaultAuthModel implements DBAAuthModel<VaultAuthCredentials>  {

	public static final String PROP_SECRET = "secret";
    public static final String PROP_ADDRESS = "address";

    @NotNull
    public VaultAuthCredentials createCredentials() {
        return new VaultAuthCredentials();
    }

    @Override
    public VaultAuthCredentials loadCredentials(DBPDataSourceContainer dataSource, DBPConnectionConfiguration configuration) {
        VaultAuthCredentials credentials = createCredentials();
        credentials.setSecret(configuration.getAuthProperty(PROP_SECRET));
        credentials.setVaultHost(configuration.getAuthProperty(PROP_ADDRESS));
        return credentials;
    }

    @Override
    public void saveCredentials(DBPDataSourceContainer dataSource, DBPConnectionConfiguration configuration, VaultAuthCredentials credentials) {
        configuration.setAuthProperty(PROP_SECRET, credentials.getSecret());
        configuration.setAuthProperty(PROP_ADDRESS, credentials.getVaultHost());
    }

    @Override
    public Object initAuthentication(DBRProgressMonitor monitor, DBPDataSource dataSource, VaultAuthCredentials credentials, DBPConnectionConfiguration configuration, Properties connectProps) throws DBException {

        final var address = credentials.getVaultHost();
        final var secret = credentials.getSecret();

        if (address != null && secret != null) {
            try {

                ProcessBuilder builder = new ProcessBuilder("vault", "read", "-field=data", "-format=json", "-address", address, secret);
                final var process = builder.start();

                if (process.waitFor(1, TimeUnit.MINUTES)) {
                    if (process.exitValue() == 0) {
                        Gson gson = new Gson();
                        JsonReader reader = gson.newJsonReader(new InputStreamReader(process.getInputStream()));
                        reader.beginObject();
                        while (reader.hasNext()) {
                            String name = reader.nextName();
                            switch (name) {
                                case "username":
                                    String username = reader.nextString();
                                    connectProps.put(DBConstants.DATA_SOURCE_PROPERTY_USER, username);
                                    break;
                                case "password":
                                    String password = reader.nextString();
                                    connectProps.put(DBConstants.DATA_SOURCE_PROPERTY_PASSWORD, password);
                                    break;
                                default:
                                    break;
                            }
                        }
                        reader.endObject();
                    } else {
                        String error = new BufferedReader(
                                new InputStreamReader(process.getErrorStream(), StandardCharsets.UTF_8))
                                .lines()
                                .collect(Collectors.joining("\n"));
                        throw new DBException(error);
                    }
                } else {
                    throw new DBException("timeout contacting Vault");
                }

            } catch (IOException | InterruptedException e) {
                throw new DBException(e.getLocalizedMessage(), e);
            }
        }

        return credentials;
    }

    @Override
    public void endAuthentication(DBPDataSourceContainer dataSource, DBPConnectionConfiguration configuration, Properties connProperties) {

    }

    @Override
    public void refreshCredentials(DBRProgressMonitor monitor, DBPDataSourceContainer dataSource, DBPConnectionConfiguration configuration, VaultAuthCredentials credentials) throws DBException {

    }
}