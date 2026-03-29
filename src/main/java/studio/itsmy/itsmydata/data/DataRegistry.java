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
            ResolvedDataType resolvedDataType = resolveDataType(rawType, definitionSection);
            DataType dataType = resolvedDataType.dataType();

            Object defaultValue = normalizeDefaultValue(definitionSection.get("default"), rawType, resolvedDataType.dataType());
            Double minValue = definitionSection.contains("min") ? definitionSection.getDouble("min") : null;
            Double maxValue = definitionSection.contains("max") ? definitionSection.getDouble("max") : null;
            LeaderboardSettings leaderboardSettings = loadLeaderboardSettings(definitionSection);

            definitions.put(
                key.toLowerCase(),
                new DataDefinition(
                    key,
                    dataType,
                    resolvedDataType.decimal(),
                    scopeType,
                    defaultValue,
                    minValue,
                    maxValue,
                    leaderboardSettings
                )
            );
        }
    }

    private ResolvedDataType resolveDataType(String rawType, ConfigurationSection definitionSection) {
        String normalizedType = rawType.trim().toLowerCase();
        return switch (normalizedType) {
            case "integer" -> new ResolvedDataType(DataType.NUMBER, false);
            case "double" -> new ResolvedDataType(DataType.NUMBER, true);
            case "boolean" -> new ResolvedDataType(DataType.STRING, false);
            case "number" -> new ResolvedDataType(DataType.NUMBER, definitionSection.getBoolean("decimal", false));
            case "string" -> new ResolvedDataType(DataType.STRING, false);
            default -> throw new IllegalArgumentException("Unknown data type: " + rawType);
        };
    }

    private Object normalizeDefaultValue(Object defaultValue, String rawType, DataType dataType) {
        if (defaultValue == null) {
            return null;
        }
        if (dataType == DataType.STRING && rawType.trim().equalsIgnoreCase("boolean")) {
            return String.valueOf(defaultValue);
        }
        return defaultValue;
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

    private record ResolvedDataType(DataType dataType, boolean decimal) {
    }
}
