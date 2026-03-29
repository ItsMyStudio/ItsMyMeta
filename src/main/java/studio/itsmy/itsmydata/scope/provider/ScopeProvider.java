package studio.itsmy.itsmydata.scope.provider;

import java.util.List;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import studio.itsmy.itsmydata.scope.CommandResolvedScope;
import studio.itsmy.itsmydata.scope.ResolvedScope;
import studio.itsmy.itsmydata.scope.ScopeType;

public interface ScopeProvider {

    ScopeType type();

    ResolvedScope resolveFromSender(CommandSender sender);

    ResolvedScope resolveFromPlayer(OfflinePlayer player);

    CommandResolvedScope resolveFromCommand(CommandSender sender, String target, boolean requireExplicitTarget);

    List<String> suggestTargets();

    String displayName(String scopeId);
}
