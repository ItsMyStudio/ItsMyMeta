package studio.itsmy.itsmydata.scope;

import java.util.List;
import java.util.stream.Stream;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public final class WorldScopeProvider implements ScopeProvider {

    @Override
    public ScopeType type() {
        return ScopeType.WORLD;
    }

    @Override
    public ResolvedScope resolveFromSender(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            throw new IllegalStateException("World scope requires an in-game player.");
        }
        return new ResolvedScope(new ScopeContext(ScopeType.WORLD, player.getWorld().getName()), player, player.getWorld().getName());
    }

    @Override
    public ResolvedScope resolveFromPlayer(OfflinePlayer player) {
        Player onlinePlayer = player.getPlayer();
        if (onlinePlayer == null) {
            throw new IllegalStateException("World scope requires the player to be online.");
        }
        return new ResolvedScope(new ScopeContext(ScopeType.WORLD, onlinePlayer.getWorld().getName()), onlinePlayer, onlinePlayer.getWorld().getName());
    }

    @Override
    public CommandResolvedScope resolveFromCommand(CommandSender sender, String target, boolean requireExplicitTarget) {
        if (target == null || target.isBlank()) {
            if (requireExplicitTarget) {
                throw new IllegalArgumentException("Missing target for world-scoped data.");
            }
            return new CommandResolvedScope(resolveFromSender(sender), false);
        }

        World world = Bukkit.getWorld(target);
        if (world != null) {
            return new CommandResolvedScope(new ResolvedScope(new ScopeContext(ScopeType.WORLD, world.getName()), null, world.getName()), true);
        }

        Player player = Bukkit.getPlayerExact(target);
        if (player != null) {
            return new CommandResolvedScope(
                new ResolvedScope(new ScopeContext(ScopeType.WORLD, player.getWorld().getName()), player, player.getWorld().getName()),
                true
            );
        }

        throw new IllegalArgumentException("Unknown world or online player: " + target);
    }

    @Override
    public List<String> suggestTargets() {
        return Stream.concat(
            Bukkit.getWorlds().stream().map(World::getName),
            Bukkit.getOnlinePlayers().stream().map(Player::getName)
        ).distinct().toList();
    }

    @Override
    public String displayName(String scopeId) {
        return scopeId;
    }
}
