package studio.itsmy.itsmydata.scope.provider;

import java.util.EnumMap;
import java.util.Map;
import org.bukkit.command.CommandSender;
import org.bukkit.OfflinePlayer;
import studio.itsmy.itsmydata.scope.*;

public final class ScopeProviders {

    private final Map<ScopeType, ScopeProvider> providers;

    public ScopeProviders() {
        this.providers = new EnumMap<>(ScopeType.class);
        register(new GlobalScopeProvider());
        register(new PlayerScopeProvider());
        register(new WorldScopeProvider());
        register(new SuperiorSkyblockScopeProvider());
    }

    public ScopeProvider get(ScopeType scopeType) {
        ScopeProvider provider = providers.get(scopeType);
        if (provider == null) {
            throw new IllegalStateException(
                "Scope '" + scopeType.name().toLowerCase() + "' is declared, but no external provider is connected yet."
            );
        }
        return provider;
    }

    public ResolvedScope resolveFromSender(CommandSender sender, ScopeType scopeType) {
        return get(scopeType).resolveFromSender(sender);
    }

    public ResolvedScope resolveFromPlayer(OfflinePlayer player, ScopeType scopeType) {
        return get(scopeType).resolveFromPlayer(player);
    }

    public CommandResolvedScope resolveFromCommand(CommandSender sender, ScopeType scopeType, String target, boolean requireExplicitTarget) {
        return get(scopeType).resolveFromCommand(sender, target, requireExplicitTarget);
    }

    private void register(ScopeProvider provider) {
        providers.put(provider.type(), provider);
    }
}
