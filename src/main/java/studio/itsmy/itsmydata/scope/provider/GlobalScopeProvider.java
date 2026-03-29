package studio.itsmy.itsmydata.scope.provider;

import java.util.List;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import studio.itsmy.itsmydata.scope.*;

public final class GlobalScopeProvider implements ScopeProvider {

    @Override
    public ScopeType type() {
        return ScopeType.GLOBAL;
    }

    @Override
    public ResolvedScope resolveFromSender(CommandSender sender) {
        return new ResolvedScope(globalScope(), sender instanceof Player player ? player : null, "global");
    }

    @Override
    public ResolvedScope resolveFromPlayer(OfflinePlayer player) {
        return new ResolvedScope(globalScope(), player, "global");
    }

    @Override
    public CommandResolvedScope resolveFromCommand(CommandSender sender, String target, boolean requireExplicitTarget) {
        return new CommandResolvedScope(resolveFromSender(sender), false);
    }

    @Override
    public List<String> suggestTargets() {
        return List.of();
    }

    @Override
    public String displayName(String scopeId) {
        return "global";
    }

    private ScopeContext globalScope() {
        return new ScopeContext(ScopeType.GLOBAL, "global");
    }
}
