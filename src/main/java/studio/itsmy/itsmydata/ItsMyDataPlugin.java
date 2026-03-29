package studio.itsmy.itsmydata;

import java.util.Objects;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import studio.itsmy.itsmydata.commands.DataCommand;
import studio.itsmy.itsmydata.db.DatabaseSettings;
import studio.itsmy.itsmydata.db.DatabaseSettingsLoader;
import studio.itsmy.itsmydata.db.JdbcDataStore;
import studio.itsmy.itsmydata.leaderboard.LeaderboardService;
import studio.itsmy.itsmydata.listener.SuperiorSkyblockCleanupListener;
import studio.itsmy.itsmydata.message.MessageService;
import studio.itsmy.itsmydata.data.DataRegistry;
import studio.itsmy.itsmydata.data.DataService;
import studio.itsmy.itsmydata.papi.ItsMyDataExpansion;
import studio.itsmy.itsmydata.scope.ScopeResolver;

public final class ItsMyDataPlugin extends JavaPlugin {

    private DataRegistry dataRegistry;
    private DataService dataService;
    private JdbcDataStore dataStore;
    private MessageService messageService;
    private LeaderboardService leaderboardService;
    private BukkitTask dynamicLeaderboardRefreshTask;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        this.messageService = new MessageService(this);
        messageService.load("lang.yml");

        this.dataRegistry = new DataRegistry(getLogger());
        dataRegistry.load(getConfig());

        DatabaseSettings databaseSettings = new DatabaseSettingsLoader().load(getConfig());
        this.dataStore = new JdbcDataStore(this, databaseSettings);
        dataStore.initialize();

        ScopeResolver scopeResolver = new ScopeResolver();
        this.leaderboardService = new LeaderboardService(dataStore);
        this.dataService = new DataService(dataRegistry, scopeResolver, dataStore, leaderboardService);
        dataService.validateDefinitions();

        PluginCommand command = Objects.requireNonNull(getCommand("data"), "data command is not defined");
        DataCommand dataCommand = new DataCommand(dataService, messageService, this::reloadPluginState);
        command.setExecutor(dataCommand);
        command.setTabCompleter(dataCommand);
        registerScopeCleanupListeners();
        registerPlaceholderExpansion();

        messageService.info(
            "logs.plugin-enabled",
            messageService.placeholder("count", dataRegistry.size()),
            messageService.placeholder("database_type", databaseSettings.type().name().toLowerCase())
        );
        messageService.info(
            "logs.leaderboards-enabled",
            messageService.placeholder("count", dataService.getEnabledLeaderboardCount())
        );
        restartDynamicLeaderboardRefreshTask();
    }

    @Override
    public void onDisable() {
        if (dynamicLeaderboardRefreshTask != null) {
            dynamicLeaderboardRefreshTask.cancel();
        }
        if (dataStore != null) {
            dataStore.close();
        }
    }

    private void registerPlaceholderExpansion() {
        if (getServer().getPluginManager().getPlugin("PlaceholderAPI") == null) {
            return;
        }

        new ItsMyDataExpansion(this, dataService).register();
        messageService.info("logs.placeholderapi-registered");
    }

    private void registerScopeCleanupListeners() {
        if (getServer().getPluginManager().getPlugin("SuperiorSkyblock2") != null) {
            getServer().getPluginManager().registerEvents(new SuperiorSkyblockCleanupListener(dataStore), this);
        }
    }

    private void reloadPluginState() {
        reloadConfig();
        messageService.load("lang.yml");
        dataRegistry.load(getConfig());
        dataService.validateDefinitions();
        restartDynamicLeaderboardRefreshTask();
        messageService.info("logs.plugin-reloaded", messageService.placeholder("count", dataRegistry.size()));
    }

    private void restartDynamicLeaderboardRefreshTask() {
        if (dynamicLeaderboardRefreshTask != null) {
            dynamicLeaderboardRefreshTask.cancel();
        }
        long refreshMinutes = dataService.getLowestDynamicRefreshMinutes();
        if (refreshMinutes <= 0) {
            return;
        }

        long periodTicks = refreshMinutes * 60L * 20L;
        this.dynamicLeaderboardRefreshTask = getServer().getScheduler().runTaskTimerAsynchronously(
            this,
            dataService::refreshDynamicLeaderboards,
            periodTicks,
            periodTicks
        );
        messageService.info(
            "logs.dynamic-leaderboard-refresh",
            messageService.placeholder("minutes", refreshMinutes)
        );
    }

    public MessageService getMessageService() {
        return messageService;
    }
}
