package studio.itsmy.itsmydata.scope.provider;

import com.bgsoftware.superiorskyblock.api.SuperiorSkyblockAPI;
import com.bgsoftware.superiorskyblock.api.island.Island;
import com.bgsoftware.superiorskyblock.api.wrappers.SuperiorPlayer;
import java.util.List;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import studio.itsmy.itsmydata.scope.CommandResolvedScope;
import studio.itsmy.itsmydata.scope.ResolvedScope;
import studio.itsmy.itsmydata.scope.ScopeContext;
import studio.itsmy.itsmydata.scope.ScopeType;

public final class SuperiorSkyblockScopeProvider implements ScopeProvider {

    @Override
    public ScopeType type() {
        return ScopeType.SUPERIORSKYBLOCK;
    }

    @Override
    public ResolvedScope resolveFromSender(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            throw new IllegalStateException("SuperiorSkyblock scope requires an in-game player.");
        }
        return resolveFromPlayer(player);
    }

    @Override
    public ResolvedScope resolveFromPlayer(OfflinePlayer player) {
        ensureAvailable();
        SuperiorPlayer superiorPlayer = resolveSuperiorPlayer(player);
        Island island = superiorPlayer.getIsland();
        if (island == null) {
            throw new IllegalStateException("Player '" + player.getName() + "' is not part of a SuperiorSkyblock island.");
        }
        return new ResolvedScope(
            new ScopeContext(ScopeType.SUPERIORSKYBLOCK, island.getUniqueId().toString()),
            player,
            "island of " + displayName(player)
        );
    }

    @Override
    public CommandResolvedScope resolveFromCommand(CommandSender sender, String target, boolean requireExplicitTarget) {
        if (target == null || target.isBlank()) {
            if (requireExplicitTarget) {
                throw new IllegalArgumentException("Missing target for SuperiorSkyblock-scoped data.");
            }
            return new CommandResolvedScope(resolveFromSender(sender), false);
        }
        Player onlinePlayer = Bukkit.getPlayerExact(target);
        if (onlinePlayer != null) {
            return new CommandResolvedScope(resolveFromPlayer(onlinePlayer), true);
        }
        OfflinePlayer cachedPlayer = Bukkit.getOfflinePlayerIfCached(target);
        if (cachedPlayer != null) {
            return new CommandResolvedScope(resolveFromPlayer(cachedPlayer), true);
        }
        throw new IllegalArgumentException("Unknown player: " + target);
    }

    @Override
    public List<String> suggestTargets() {
        return Bukkit.getOnlinePlayers().stream().map(Player::getName).toList();
    }

    @Override
    public String displayName(String scopeId) {
        if (Bukkit.getPluginManager().getPlugin("SuperiorSkyblock2") == null) {
            return scopeId;
        }

        try {
            Island island = SuperiorSkyblockAPI.getIslandByUUID(UUID.fromString(scopeId));
            if (island == null) {
                return scopeId;
            }

            String islandName = island.getName();
            if (islandName != null && !islandName.isBlank()) {
                return islandName;
            }

            SuperiorPlayer owner = island.getOwner();
            if (owner != null && owner.asOfflinePlayer() != null) {
                return "island of " + displayName(owner.asOfflinePlayer());
            }
        } catch (IllegalArgumentException ignored) {
            return scopeId;
        }

        return scopeId;
    }

    private void ensureAvailable() {
        if (Bukkit.getPluginManager().getPlugin("SuperiorSkyblock2") == null) {
            throw new IllegalStateException("Scope 'superiorskyblock' is declared, but SuperiorSkyblock2 is not installed.");
        }
    }

    private SuperiorPlayer resolveSuperiorPlayer(OfflinePlayer player) {
        SuperiorPlayer superiorPlayer = SuperiorSkyblockAPI.getPlayer(player.getUniqueId());
        if (superiorPlayer == null && player.getName() != null) {
            superiorPlayer = SuperiorSkyblockAPI.getPlayer(player.getName());
        }
        if (superiorPlayer == null) {
            throw new IllegalStateException("Player '" + player.getName() + "' is not loaded in SuperiorSkyblock2.");
        }
        return superiorPlayer;
    }

    private String displayName(OfflinePlayer player) {
        return player.getName() != null ? player.getName() : player.getUniqueId().toString();
    }
}
