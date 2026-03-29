package studio.itsmy.itsmydata.scope;

import org.bukkit.OfflinePlayer;

public record ResolvedScope(ScopeContext scope, OfflinePlayer player, String displayTarget) {
}
