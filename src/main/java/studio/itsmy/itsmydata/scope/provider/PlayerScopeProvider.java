package studio.itsmy.itsmydata.scope.provider;

import java.util.List;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import studio.itsmy.itsmydata.scope.*;

public final class PlayerScopeProvider implements ScopeProvider {

    @Override
    public ScopeType type() {
        return ScopeType.PLAYER;
    }

    @Override
    public ResolvedScope resolveFromSender(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            throw new IllegalStateException("Player scope requires an in-game player.");
        }
        return new ResolvedScope(new ScopeContext(ScopeType.PLAYER, player.getUniqueId().toString()), player, displayName(player));
    }

    @Override
    public ResolvedScope resolveFromPlayer(OfflinePlayer player) {
        return new ResolvedScope(new ScopeContext(ScopeType.PLAYER, player.getUniqueId().toString()), player, displayName(player));
    }

    @Override
    public CommandResolvedScope resolveFromCommand(CommandSender sender, String target, boolean requireExplicitTarget) {
        if (target == null || target.isBlank()) {
            if (requireExplicitTarget) {
                throw new IllegalArgumentException("Missing target for player-scoped data.");
            }
            return new CommandResolvedScope(resolveFromSender(sender), false);
        }
        OfflinePlayer player = resolvePlayer(target);
        return new CommandResolvedScope(resolveFromPlayer(player), true);
    }

    @Override
    public List<String> suggestTargets() {
        return Bukkit.getOnlinePlayers().stream().map(Player::getName).toList();
    }

    @Override
    public String displayName(String scopeId) {
        try {
            return displayName(UUID.fromString(scopeId));
        } catch (IllegalArgumentException ignored) {
            return scopeId;
        }
    }

    private OfflinePlayer resolvePlayer(String input) {
        try {
            return Bukkit.getOfflinePlayer(UUID.fromString(input));
        } catch (IllegalArgumentException ignored) {
            Player onlinePlayer = Bukkit.getPlayerExact(input);
            if (onlinePlayer != null) {
                return onlinePlayer;
            }

            OfflinePlayer cachedPlayer = Bukkit.getOfflinePlayerIfCached(input);
            if (cachedPlayer != null) {
                return cachedPlayer;
            }
            throw new IllegalArgumentException("Unknown player: " + input);
        }
    }

    private String displayName(OfflinePlayer player) {
        return player.getName() != null ? player.getName() : player.getUniqueId().toString();
    }

    private String displayName(UUID uniqueId) {
        Player onlinePlayer = Bukkit.getPlayer(uniqueId);
        if (onlinePlayer != null) {
            return onlinePlayer.getName();
        }

        for (OfflinePlayer offlinePlayer : Bukkit.getOfflinePlayers()) {
            if (offlinePlayer.getUniqueId().equals(uniqueId) && offlinePlayer.getName() != null) {
                return offlinePlayer.getName();
            }
        }

        OfflinePlayer fallback = Bukkit.getOfflinePlayer(uniqueId);
        return displayName(fallback);
    }
}
