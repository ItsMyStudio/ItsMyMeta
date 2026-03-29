package studio.itsmy.itsmydata.listener;

import com.bgsoftware.superiorskyblock.api.events.IslandDisbandEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import studio.itsmy.itsmydata.data.DataStore;
import studio.itsmy.itsmydata.scope.ScopeContext;
import studio.itsmy.itsmydata.scope.ScopeType;

public final class SuperiorSkyblockCleanupListener implements Listener {

    private final DataStore dataStore;

    public SuperiorSkyblockCleanupListener(DataStore dataStore) {
        this.dataStore = dataStore;
    }

    @EventHandler(ignoreCancelled = true)
    public void onIslandDisband(IslandDisbandEvent event) {
        dataStore.deleteScope(new ScopeContext(ScopeType.SUPERIORSKYBLOCK, event.getIsland().getUniqueId().toString()));
    }
}
