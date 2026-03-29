package com.premiumminds.vault.client;

import com.google.gson.Gson;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class DefaultVaultTokenLoader implements VaultTokenLoader {

    private static final String ENV_VAULT_ADDR = "VAULT_ADDR";
    private static final String ENV_VAULT_CONFIG_PATH = "VAULT_CONFIG_PATH";
    private static final String DEFAULT_VAULT_CONFIG_FILE = ".vault";
    private static final String DEFAULT_VAULT_TOKEN_FILE = ".vault-token";
    private static final String ERROR_VAULT_TOKEN_NOT_DEFINED = "Vault token not defined";

    private final Optional<Path> tokenFile;
    private final String vaultAddress;

    public DefaultVaultTokenLoader(Optional<Path> tokenFile, String vaultAddress) {
        this.tokenFile = tokenFile;
        this.vaultAddress = vaultAddress;
    }

    @Override
    public String get() throws Exception {
        if (tokenFile.isPresent() && !tokenFile.toString().isBlank()) {
            if (tokenFile.get().toFile().exists()){
                return Files.readString(tokenFile.get());
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

        throw new Exception(ERROR_VAULT_TOKEN_NOT_DEFINED);
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

        try (FileReader fileReader = new FileReader(configFile.toFile())) {
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
