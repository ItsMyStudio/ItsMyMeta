package studio.itsmy.itsmydata.data;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Logger;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import studio.itsmy.itsmydata.leaderboard.LeaderboardSettings;
import studio.itsmy.itsmydata.scope.ScopeType;

public final class DataRegistry {

    private final Logger logger;
    private final Map<String, DataDefinition> definitions = new LinkedHashMap<>();

    public DataRegistry(Logger logger) {
        this.logger = logger;
    }

    public void load(FileConfiguration configuration) {
        definitions.clear();

        ConfigurationSection dataSection = configuration.getConfigurationSection("data-definitions");
        if (dataSection == null) {
            logger.warning("No 'data-definitions' section found in config.yml");
            return;
        }

        for (String key : dataSection.getKeys(false)) {
            ConfigurationSection definitionSection = dataSection.getConfigurationSection(key);
            if (definitionSection == null) {
                logger.warning("Skipping data '" + key + "' because it is not a section.");
                continue;
            }

            String rawScope = definitionSection.getString("scope", "global");
            ScopeType scopeType = ScopeType.fromConfigValue(rawScope);

            String rawType = definitionSection.getString("type", "string");
            DataType dataType = DataType.fromConfigValue(rawType);

            boolean decimal = definitionSection.getBoolean("decimal", false);

            Object defaultValue = definitionSection.get("default");
            Double minValue = definitionSection.contains("min") ? definitionSection.getDouble("min") : null;
            Double maxValue = definitionSection.contains("max") ? definitionSection.getDouble("max") : null;
            LeaderboardSettings leaderboardSettings = loadLeaderboardSettings(definitionSection);

            definitions.put(
                key.toLowerCase(),
                new DataDefinition(
                    key,
                    dataType,
                    decimal,
                    scopeType,
                    defaultValue,
                    minValue,
                    maxValue,
                    leaderboardSettings
                )
            );
        }
    }

    private LeaderboardSettings loadLeaderboardSettings(ConfigurationSection definitionSection) {
        ConfigurationSection section = definitionSection.getConfigurationSection("leaderboard");
        if (section == null) {
            return LeaderboardSettings.DISABLED;
        }

        boolean enabled = section.getBoolean("enabled", true);
        int exactRankLimit = Math.max(1, section.getInt("exact-rank-limit", 1000));
        int dynamicRefreshMinutes = Math.max(0, section.getInt("dynamic-refresh-minutes", 5));
        return new LeaderboardSettings(enabled, exactRankLimit, dynamicRefreshMinutes);
    }

    public DataDefinition getRequired(String key) {
        DataDefinition definition = definitions.get(key.toLowerCase());
        if (definition == null) {
            throw new IllegalArgumentException("Unknown data key: " + key);
        }
        return definition;
    }

    public int size() {
        return definitions.size();
    }

    public Collection<DataDefinition> all() {
        return definitions.values();
    }
}
