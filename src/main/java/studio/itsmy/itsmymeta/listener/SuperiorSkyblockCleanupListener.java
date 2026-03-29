package studio.itsmy.itsmymeta.listener;

import com.bgsoftware.superiorskyblock.api.events.IslandDisbandEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import studio.itsmy.itsmymeta.meta.MetaStore;
import studio.itsmy.itsmymeta.scope.ScopeContext;
import studio.itsmy.itsmymeta.scope.ScopeType;

public final class SuperiorSkyblockCleanupListener implements Listener {

    private final MetaStore metaStore;

    public SuperiorSkyblockCleanupListener(MetaStore metaStore) {
        this.metaStore = metaStore;
    }

    @EventHandler(ignoreCancelled = true)
    public void onIslandDisband(IslandDisbandEvent event) {
        metaStore.deleteScope(new ScopeContext(ScopeType.SUPERIORSKYBLOCK, event.getIsland().getUniqueId().toString()));
    }
}
