package studio.itsmy.itsmymeta;

import java.util.Objects;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import studio.itsmy.itsmymeta.commands.MetaCommand;
import studio.itsmy.itsmymeta.db.DatabaseSettings;
import studio.itsmy.itsmymeta.db.DatabaseSettingsLoader;
import studio.itsmy.itsmymeta.db.JdbcMetaStore;
import studio.itsmy.itsmymeta.leaderboard.LeaderboardService;
import studio.itsmy.itsmymeta.listener.SuperiorSkyblockCleanupListener;
import studio.itsmy.itsmymeta.message.MessageService;
import studio.itsmy.itsmymeta.meta.MetaRegistry;
import studio.itsmy.itsmymeta.meta.MetaService;
import studio.itsmy.itsmymeta.papi.ItsMyMetaExpansion;
import studio.itsmy.itsmymeta.scope.ScopeResolver;

public final class ItsMyMetaPlugin extends JavaPlugin {

    private MetaRegistry metaRegistry;
    private MetaService metaService;
    private JdbcMetaStore metaStore;
    private MessageService messageService;
    private LeaderboardService leaderboardService;
    private BukkitTask dynamicLeaderboardRefreshTask;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        this.messageService = new MessageService(this);
        messageService.load("lang.yml");

        this.metaRegistry = new MetaRegistry(getLogger());
        metaRegistry.load(getConfig());

        DatabaseSettings databaseSettings = new DatabaseSettingsLoader().load(getConfig());
        this.metaStore = new JdbcMetaStore(this, databaseSettings);
        metaStore.initialize();

        ScopeResolver scopeResolver = new ScopeResolver();
        this.leaderboardService = new LeaderboardService(metaStore);
        this.metaService = new MetaService(metaRegistry, scopeResolver, metaStore, leaderboardService);
        metaService.validateDefinitions();

        PluginCommand command = Objects.requireNonNull(getCommand("meta"), "meta command is not defined");
        MetaCommand metaCommand = new MetaCommand(metaService, messageService, this::reloadPluginState);
        command.setExecutor(metaCommand);
        command.setTabCompleter(metaCommand);
        registerScopeCleanupListeners();
        registerPlaceholderExpansion();

        messageService.info(
            "logs.plugin-enabled",
            messageService.placeholder("count", metaRegistry.size()),
            messageService.placeholder("database_type", databaseSettings.type().name().toLowerCase())
        );
        messageService.info(
            "logs.leaderboards-enabled",
            messageService.placeholder("count", metaService.getEnabledLeaderboardCount())
        );
        restartDynamicLeaderboardRefreshTask();
    }

    @Override
    public void onDisable() {
        if (dynamicLeaderboardRefreshTask != null) {
            dynamicLeaderboardRefreshTask.cancel();
        }
        if (metaStore != null) {
            metaStore.close();
        }
    }

    private void registerPlaceholderExpansion() {
        if (getServer().getPluginManager().getPlugin("PlaceholderAPI") == null) {
            return;
        }

        new ItsMyMetaExpansion(this, metaService).register();
        messageService.info("logs.placeholderapi-registered");
    }

    private void registerScopeCleanupListeners() {
        if (getServer().getPluginManager().getPlugin("SuperiorSkyblock2") != null) {
            getServer().getPluginManager().registerEvents(new SuperiorSkyblockCleanupListener(metaStore), this);
        }
    }

    private void reloadPluginState() {
        reloadConfig();
        messageService.load("lang.yml");
        metaRegistry.load(getConfig());
        metaService.validateDefinitions();
        restartDynamicLeaderboardRefreshTask();
        messageService.info("logs.plugin-reloaded", messageService.placeholder("count", metaRegistry.size()));
    }

    private void restartDynamicLeaderboardRefreshTask() {
        if (dynamicLeaderboardRefreshTask != null) {
            dynamicLeaderboardRefreshTask.cancel();
        }
        long refreshMinutes = metaService.getLowestDynamicRefreshMinutes();
        if (refreshMinutes <= 0) {
            return;
        }

        long periodTicks = refreshMinutes * 60L * 20L;
        this.dynamicLeaderboardRefreshTask = getServer().getScheduler().runTaskTimerAsynchronously(
            this,
            metaService::refreshDynamicLeaderboards,
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
