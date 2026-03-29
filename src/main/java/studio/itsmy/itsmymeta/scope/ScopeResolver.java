package studio.itsmy.itsmymeta.scope;

import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;

public final class ScopeResolver {

    private final ScopeProviders scopeProviders;

    public ScopeResolver() {
        this.scopeProviders = new ScopeProviders();
    }

    public ScopeContext resolve(CommandSender sender, ScopeType scopeType) {
        return scopeProviders.resolveFromSender(sender, scopeType).scope();
    }

    public ScopeContext resolve(OfflinePlayer player, ScopeType scopeType) {
        return scopeProviders.resolveFromPlayer(player, scopeType).scope();
    }

    public String displayName(ScopeContext scope) {
        return scopeProviders.get(scope.type()).displayName(scope.id());
    }
}
