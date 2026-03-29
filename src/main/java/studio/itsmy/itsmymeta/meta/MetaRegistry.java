package studio.itsmy.itsmymeta.meta;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Logger;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import studio.itsmy.itsmymeta.leaderboard.LeaderboardSettings;
import studio.itsmy.itsmymeta.scope.ScopeType;

public final class MetaRegistry {

    private final Logger logger;
    private final Map<String, MetaDefinition> definitions = new LinkedHashMap<>();

    public MetaRegistry(Logger logger) {
        this.logger = logger;
    }

    public void load(FileConfiguration configuration) {
        definitions.clear();

        ConfigurationSection metaSection = configuration.getConfigurationSection("meta-definitions");
        if (metaSection == null) {
            logger.warning("No 'meta-definitions' section found in config.yml");
            return;
        }

        for (String key : metaSection.getKeys(false)) {
            ConfigurationSection definitionSection = metaSection.getConfigurationSection(key);
            if (definitionSection == null) {
                logger.warning("Skipping meta '" + key + "' because it is not a section.");
                continue;
            }

            String rawScope = definitionSection.getString("scope", "global");
            ScopeType scopeType = ScopeType.fromConfigValue(rawScope);

            String rawType = definitionSection.getString("type", "string");
            ResolvedMetaType resolvedMetaType = resolveMetaType(rawType, definitionSection);
            MetaType metaType = resolvedMetaType.metaType();

            Object defaultValue = normalizeDefaultValue(definitionSection.get("default"), rawType, resolvedMetaType.metaType());
            Double minValue = definitionSection.contains("min") ? definitionSection.getDouble("min") : null;
            Double maxValue = definitionSection.contains("max") ? definitionSection.getDouble("max") : null;
            LeaderboardSettings leaderboardSettings = loadLeaderboardSettings(definitionSection);

            definitions.put(
                key.toLowerCase(),
                new MetaDefinition(
                    key,
                    metaType,
                    resolvedMetaType.decimal(),
                    scopeType,
                    defaultValue,
                    minValue,
                    maxValue,
                    leaderboardSettings
                )
            );
        }
    }

    private ResolvedMetaType resolveMetaType(String rawType, ConfigurationSection definitionSection) {
        String normalizedType = rawType.trim().toLowerCase();
        return switch (normalizedType) {
            case "integer" -> new ResolvedMetaType(MetaType.NUMBER, false);
            case "double" -> new ResolvedMetaType(MetaType.NUMBER, true);
            case "boolean" -> new ResolvedMetaType(MetaType.STRING, false);
            case "number" -> new ResolvedMetaType(MetaType.NUMBER, definitionSection.getBoolean("decimal", false));
            case "string" -> new ResolvedMetaType(MetaType.STRING, false);
            default -> throw new IllegalArgumentException("Unknown meta type: " + rawType);
        };
    }

    private Object normalizeDefaultValue(Object defaultValue, String rawType, MetaType metaType) {
        if (defaultValue == null) {
            return null;
        }
        if (metaType == MetaType.STRING && rawType.trim().equalsIgnoreCase("boolean")) {
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

    public MetaDefinition getRequired(String key) {
        MetaDefinition definition = definitions.get(key.toLowerCase());
        if (definition == null) {
            throw new IllegalArgumentException("Unknown meta key: " + key);
        }
        return definition;
    }

    public int size() {
        return definitions.size();
    }

    public Collection<MetaDefinition> all() {
        return definitions.values();
    }

    private record ResolvedMetaType(MetaType metaType, boolean decimal) {
    }
}
