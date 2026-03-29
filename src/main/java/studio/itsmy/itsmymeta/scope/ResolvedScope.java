package studio.itsmy.itsmymeta.scope;

import org.bukkit.OfflinePlayer;

public record ResolvedScope(ScopeContext scope, OfflinePlayer placeholderPlayer, String displayTarget) {
}
