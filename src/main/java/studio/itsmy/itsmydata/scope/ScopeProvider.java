package studio.itsmy.itsmydata.scope;

import java.util.List;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;

public interface ScopeProvider {

    ScopeType type();

    ResolvedScope resolveFromSender(CommandSender sender);

    ResolvedScope resolveFromPlayer(OfflinePlayer player);

    CommandResolvedScope resolveFromCommand(CommandSender sender, String target, boolean requireExplicitTarget);

    List<String> suggestTargets();

    String displayName(String scopeId);
}
