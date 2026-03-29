package studio.itsmy.itsmydata.scope;

import org.bukkit.OfflinePlayer;
import studio.itsmy.itsmydata.scope.provider.ScopeProviders;

public final class ScopeResolver {

    private final ScopeProviders scopeProviders;

    public ScopeResolver() {
        this.scopeProviders = new ScopeProviders();
    }

    public ScopeContext resolve(OfflinePlayer player, ScopeType scopeType) {
        return scopeProviders.resolveFromPlayer(player, scopeType).scope();
    }

    public String displayName(ScopeContext scope) {
        return scopeProviders.get(scope.type()).displayName(scope.id());
    }
}
