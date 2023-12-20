package io.quarkus.vault.client.test;

import java.net.http.HttpClient;

import org.junit.jupiter.api.extension.*;
import org.testcontainers.vault.VaultContainer;

import io.quarkus.vault.client.VaultClient;
import io.quarkus.vault.client.http.jdk.JDKVaultHttpClient;

@VaultClientTest
public class VaultClientTestExtension implements BeforeAllCallback, AfterAllCallback, ParameterResolver {

    private static final java.util.Random random = new java.util.Random();

    // Generate a random path to avoid conflicts between tests
    static String getRandomString(int length) {
        // Generate short random alphanumeric string of given length
        return random.ints(48, 122)
                .filter(i -> (i < 57 || i > 65) && (i < 90 || i > 97))
                .limit(length)
                .collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append)
                .toString();
    }

    private final VaultContainer<?> vaultContainer = new VaultContainer<>("hashicorp/vault:1.15.2")
            .withEnv("VAULT_LOG_LEVEL", "debug")
            .withVaultToken("root");
    private final JDKVaultHttpClient httpClient = new JDKVaultHttpClient(HttpClient.newHttpClient());
    private VaultClient vaultClient;

    @Override
    public void beforeAll(ExtensionContext extensionContext) throws Exception {

        var annotation = extensionContext.getRequiredTestClass().getAnnotation(VaultClientTest.class);

        vaultContainer
                .withEnv("VAULT_LOG_LEVEL", annotation.logLevel())
                .start();

        vaultClient = VaultClient.builder()
                .executor(httpClient)
                .baseUrl(vaultContainer.getHttpHostAddress())
                .clientToken("root")
                .build();

        var secretMounts = annotation.secrets();
        for (var secretMount : secretMounts) {
            var cmd = "vault secrets enable -path=" + secretMount.path() + " " +
                    String.join(" ", secretMount.options()) + " " + secretMount.type();
            var result = vaultContainer.execInContainer("/bin/sh", "-c", cmd);
            if (result.getExitCode() != 0) {
                throw new RuntimeException("Failed to enable Vault engine '" + secretMount.type() + "' at mount path '" +
                        secretMount.path() + "': " + result.getStderr());
            }
        }

        var authMounts = annotation.auths();
        for (var authMount : authMounts) {
            var cmd = "vault auth enable -path=" + authMount.path() + " " +
                    String.join(" ", authMount.options()) + " " + authMount.type();
            var result = vaultContainer.execInContainer("/bin/sh", "-c", cmd);
            if (result.getExitCode() != 0) {
                throw new RuntimeException("Failed to enable Vault auth '" + authMount.type() + "' at mount path '" +
                        authMount.path() + "': " + result.getStderr());
            }
        }
    }

    @Override
    public void afterAll(ExtensionContext extensionContext) {
        vaultContainer.stop();
    }

    @Override
    public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext)
            throws ParameterResolutionException {
        if (parameterContext.getParameter().getType().equals(VaultClient.class)) {
            return true;
        } else if (parameterContext.getParameter().getType().equals(String.class)
                && parameterContext.getParameter().getAnnotation(Random.class) != null) {
            return true;
        }
        return false;
    }

    @Override
    public Object resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext)
            throws ParameterResolutionException {
        if (parameterContext.getParameter().getType().equals(VaultClient.class)) {
            return vaultClient;
        } else if (parameterContext.getParameter().getType().equals(String.class)
                && parameterContext.getParameter().getAnnotation(Random.class) != null) {
            return getRandomString(8);
        }
        return null;
    }
}
