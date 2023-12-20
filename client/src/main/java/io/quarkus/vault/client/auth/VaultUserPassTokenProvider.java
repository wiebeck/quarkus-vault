package io.quarkus.vault.client.auth;

import java.util.function.Function;

import io.quarkus.vault.client.api.auth.userpass.VaultAuthUserPass;
import io.quarkus.vault.client.common.VaultResponse;
import io.smallrye.mutiny.Uni;

public class VaultUserPassTokenProvider implements VaultTokenProvider {
    private final String mountPath;
    private final String username;
    private final Function<VaultAuthRequest, Uni<String>> passwordProvider;

    public VaultUserPassTokenProvider(String mountPath, String username,
            Function<VaultAuthRequest, Uni<String>> passwordProvider) {
        this.mountPath = mountPath;
        this.username = username;
        this.passwordProvider = passwordProvider;
    }

    public VaultUserPassTokenProvider(VaultUserPassAuthOptions options) {
        this(options.mountPath, options.username, options.passwordProvider);
    }

    @Override
    public Uni<VaultToken> apply(VaultAuthRequest authRequest) {
        var executor = authRequest.executor();
        return passwordProvider.apply(authRequest).flatMap(password -> {
            return executor.execute(VaultAuthUserPass.FACTORY.login(mountPath, username, password))
                    .map(VaultResponse::getResult)
                    .map(res -> VaultToken.from(res.auth.clientToken, res.auth.renewable, res.auth.leaseDuration));
        });
    }
}
