package studio.itsmy.itsmydata.data;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import studio.itsmy.itsmydata.leaderboard.LeaderboardEntry;
import studio.itsmy.itsmydata.leaderboard.LeaderboardPlacement;
import studio.itsmy.itsmydata.leaderboard.LeaderboardService;
import studio.itsmy.itsmydata.leaderboard.LeaderboardSettings;
import studio.itsmy.itsmydata.papi.PlaceholderValueResolver;
import studio.itsmy.itsmydata.scope.ScopeContext;
import studio.itsmy.itsmydata.scope.ScopeResolver;
import studio.itsmy.itsmydata.scope.ScopeType;

public final class DataService {

    private final DataRegistry dataRegistry;
    private final ScopeResolver scopeResolver;
    private final DataStore dataStore;
    private final LeaderboardService leaderboardService;
    private final DataValueParser valueParser;
    private final PlaceholderValueResolver placeholderValueResolver;
    private final NumericExpressionResolver numericExpressionResolver;
    private final ConcurrentMap<String, Long> dynamicLeaderboardRefreshTimestamps;

    public DataService(DataRegistry dataRegistry, ScopeResolver scopeResolver, DataStore dataStore, LeaderboardService leaderboardService) {
        this.dataRegistry = dataRegistry;
        this.scopeResolver = scopeResolver;
        this.dataStore = dataStore;
        this.leaderboardService = leaderboardService;
        this.valueParser = new DataValueParser();
        this.placeholderValueResolver = new PlaceholderValueResolver();
        this.numericExpressionResolver = new NumericExpressionResolver();
        this.dynamicLeaderboardRefreshTimestamps = new ConcurrentHashMap<>();
    }

    public Object getValue(CommandSender sender, String dataKey) {
        DataDefinition definition = dataRegistry.getRequired(dataKey);
        ScopeContext scope = scopeResolver.resolve(sender, definition.scopeType());
        return getValue(sender, scope, definition);
    }

    public Object getValue(OfflinePlayer player, String dataKey) {
        DataDefinition definition = dataRegistry.getRequired(dataKey);
        ScopeContext scope = scopeResolver.resolve(player, definition.scopeType());
        return getValue(player, scope, definition);
    }

    public Object getValue(CommandSender sender, ScopeContext scope, String dataKey) {
        return getValue(sender, scope, dataRegistry.getRequired(dataKey));
    }

    public Object getValue(OfflinePlayer player, ScopeContext scope, String dataKey) {
        return getValue(player, scope, dataRegistry.getRequired(dataKey));
    }

    public Object getDefaultValue(CommandSender sender, String dataKey) {
        DataDefinition definition = dataRegistry.getRequired(dataKey);
        return getDefaultValue(sender, definition, definition.scopeType() == ScopeType.GLOBAL ? new ScopeContext(ScopeType.GLOBAL, "global") : scopeResolver.resolve(sender, definition.scopeType()));
    }

    public Object getDefaultValue(OfflinePlayer player, String dataKey) {
        DataDefinition definition = dataRegistry.getRequired(dataKey);
        return getDefaultValue(player, dataKey, scopeResolver.resolve(player, definition.scopeType()));
    }

    public Object getDefaultValue(CommandSender sender, String dataKey, ScopeContext scope) {
        return getDefaultValue(sender, dataRegistry.getRequired(dataKey), scope);
    }

    public Object getDefaultValue(OfflinePlayer player, String dataKey, ScopeContext scope) {
        return getDefaultValue(player, dataRegistry.getRequired(dataKey), scope);
    }

    public Object setValue(CommandSender sender, String dataKey, Object value) {
        DataDefinition definition = dataRegistry.getRequired(dataKey);
        ScopeContext scope = scopeResolver.resolve(sender, definition.scopeType());
        return setValue(sender, scope, definition, value);
    }

    public Object setValue(CommandSender sender, ScopeContext scope, String dataKey, Object value) {
        return setValue(sender, scope, dataRegistry.getRequired(dataKey), value);
    }

    public Object setValue(OfflinePlayer player, ScopeContext scope, String dataKey, Object value) {
        return setValue(player, scope, dataRegistry.getRequired(dataKey), value);
    }

    public Object resetValue(CommandSender sender, String dataKey) {
        DataDefinition definition = dataRegistry.getRequired(dataKey);
        ScopeContext scope = scopeResolver.resolve(sender, definition.scopeType());
        return resetValue(sender, scope, definition);
    }

    public Object resetValue(CommandSender sender, ScopeContext scope, String dataKey) {
        return resetValue(sender, scope, dataRegistry.getRequired(dataKey));
    }

    public Object resetValue(OfflinePlayer player, ScopeContext scope, String dataKey) {
        return resetValue(player, scope, dataRegistry.getRequired(dataKey));
    }

    public Object giveValue(CommandSender sender, String dataKey, String rawAmount) {
        DataDefinition definition = dataRegistry.getRequired(dataKey);
        ScopeContext scope = scopeResolver.resolve(sender, definition.scopeType());
        return giveValue(sender, scope, definition, rawAmount);
    }

    public Object giveValue(CommandSender sender, ScopeContext scope, String dataKey, String rawAmount) {
        return giveValue(sender, scope, dataRegistry.getRequired(dataKey), rawAmount);
    }

    public Object giveValue(OfflinePlayer player, ScopeContext scope, String dataKey, String rawAmount) {
        return giveValue(player, scope, dataRegistry.getRequired(dataKey), rawAmount);
    }

    public Object takeValue(CommandSender sender, String dataKey, String rawAmount) {
        DataDefinition definition = dataRegistry.getRequired(dataKey);
        ScopeContext scope = scopeResolver.resolve(sender, definition.scopeType());
        return takeValue(sender, scope, definition, rawAmount);
    }

    public Object takeValue(CommandSender sender, ScopeContext scope, String dataKey, String rawAmount) {
        return takeValue(sender, scope, dataRegistry.getRequired(dataKey), rawAmount);
    }

    public Object takeValue(OfflinePlayer player, ScopeContext scope, String dataKey, String rawAmount) {
        return takeValue(player, scope, dataRegistry.getRequired(dataKey), rawAmount);
    }

    private Object setValue(CommandSender sender, ScopeContext scope, DataDefinition definition, Object value) {
        Object parsedValue = resolveEffectiveValue(sender, definition, value);
        validateDynamicLeaderboardSupport(definition, value);
        Double leaderboardValue = leaderboardService.supports(definition) ? toDouble(parsedValue) : null;
        dataStore.saveValueAndSyncLeaderboard(scope, definition.key(), valueParser.serialize(parsedValue), leaderboardValue);
        return parsedValue;
    }

    private Object setValue(OfflinePlayer player, ScopeContext scope, DataDefinition definition, Object value) {
        Object parsedValue = resolveEffectiveValue(player, definition, value);
        validateDynamicLeaderboardSupport(definition, value);
        Double leaderboardValue = leaderboardService.supports(definition) ? toDouble(parsedValue) : null;
        dataStore.saveValueAndSyncLeaderboard(scope, definition.key(), valueParser.serialize(parsedValue), leaderboardValue);
        return parsedValue;
    }

    private Object resetValue(CommandSender sender, ScopeContext scope, DataDefinition definition) {
        Object resolvedDefault = resolveEffectiveValue(sender, definition, definition.defaultValue());
        Double leaderboardValue = leaderboardService.supports(definition) ? toDouble(resolvedDefault) : null;
        dataStore.deleteValueAndSyncLeaderboard(scope, definition.key(), leaderboardValue);
        return resolvedDefault;
    }

    private Object resetValue(OfflinePlayer player, ScopeContext scope, DataDefinition definition) {
        Object resolvedDefault = resolveEffectiveValue(player, definition, definition.defaultValue());
        Double leaderboardValue = leaderboardService.supports(definition) ? toDouble(resolvedDefault) : null;
        dataStore.deleteValueAndSyncLeaderboard(scope, definition.key(), leaderboardValue);
        return resolvedDefault;
    }

    private Object giveValue(CommandSender sender, ScopeContext scope, DataDefinition definition, String rawAmount) {
        ensureNumeric(definition);
        Object currentValue = getValue(sender, scope, definition);
        double nextValue = toDouble(currentValue) + resolveNumericAmount(sender, definition, rawAmount);
        Object typedValue = castNumber(nextValue, definition);
        Object boundedValue = applyBounds(typedValue, definition);
        dataStore.saveValueAndSyncLeaderboard(scope, definition.key(), valueParser.serialize(boundedValue), toDouble(boundedValue));
        return boundedValue;
    }

    private Object giveValue(OfflinePlayer player, ScopeContext scope, DataDefinition definition, String rawAmount) {
        ensureNumeric(definition);
        Object currentValue = getValue(player, scope, definition);
        double nextValue = toDouble(currentValue) + resolveNumericAmount(player, definition, rawAmount);
        Object typedValue = castNumber(nextValue, definition);
        Object boundedValue = applyBounds(typedValue, definition);
        dataStore.saveValueAndSyncLeaderboard(scope, definition.key(), valueParser.serialize(boundedValue), toDouble(boundedValue));
        return boundedValue;
    }

    private Object takeValue(CommandSender sender, ScopeContext scope, DataDefinition definition, String rawAmount) {
        ensureNumeric(definition);
        Object currentValue = getValue(sender, scope, definition);
        double nextValue = toDouble(currentValue) - resolveNumericAmount(sender, definition, rawAmount);
        Object typedValue = castNumber(nextValue, definition);
        Object boundedValue = applyBounds(typedValue, definition);
        dataStore.saveValueAndSyncLeaderboard(scope, definition.key(), valueParser.serialize(boundedValue), toDouble(boundedValue));
        return boundedValue;
    }

    private Object takeValue(OfflinePlayer player, ScopeContext scope, DataDefinition definition, String rawAmount) {
        ensureNumeric(definition);
        Object currentValue = getValue(player, scope, definition);
        double nextValue = toDouble(currentValue) - resolveNumericAmount(player, definition, rawAmount);
        Object typedValue = castNumber(nextValue, definition);
        Object boundedValue = applyBounds(typedValue, definition);
        dataStore.saveValueAndSyncLeaderboard(scope, definition.key(), valueParser.serialize(boundedValue), toDouble(boundedValue));
        return boundedValue;
    }

    public DataDefinition getDefinition(String dataKey) {
        return dataRegistry.getRequired(dataKey);
    }

    public List<DataDefinition> getDefinitions() {
        return dataRegistry.all().stream().toList();
    }

    public List<String> getSupportedScopes() {
        return List.of(
            ScopeType.GLOBAL.name().toLowerCase(),
            ScopeType.PLAYER.name().toLowerCase(),
            ScopeType.WORLD.name().toLowerCase(),
            ScopeType.SUPERIORSKYBLOCK.name().toLowerCase()
        );
    }

    public LeaderboardPlacement getLeaderboardPlacement(CommandSender sender, String dataKey) {
        DataDefinition definition = dataRegistry.getRequired(dataKey);
        if (!leaderboardService.supports(definition)) {
            throw new IllegalStateException("Data '" + definition.key() + "' does not support leaderboards.");
        }

        ScopeContext scope = scopeResolver.resolve(sender, definition.scopeType());
        return leaderboardService.getPlacement(definition, scope);
    }

    public LeaderboardPlacement getLeaderboardPlacement(OfflinePlayer player, String dataKey) {
        DataDefinition definition = dataRegistry.getRequired(dataKey);
        if (!leaderboardService.supports(definition)) {
            throw new IllegalStateException("Data '" + definition.key() + "' does not support leaderboards.");
        }

        ScopeContext scope = scopeResolver.resolve(player, definition.scopeType());
        return leaderboardService.getPlacement(definition, scope);
    }

    public Object getLeaderboardValue(String dataKey, int position) {
        DataDefinition definition = dataRegistry.getRequired(dataKey);
        if (!leaderboardService.supports(definition)) {
            throw new IllegalStateException("Data '" + definition.key() + "' does not support leaderboards.");
        }

        LeaderboardEntry entry = leaderboardService.getEntry(definition.key(), position);
        return castNumber(entry.numericValue(), definition);
    }

    public String getLeaderboardName(String dataKey, int position) {
        DataDefinition definition = dataRegistry.getRequired(dataKey);
        if (!leaderboardService.supports(definition)) {
            throw new IllegalStateException("Data '" + definition.key() + "' does not support leaderboards.");
        }

        LeaderboardEntry entry = leaderboardService.getEntry(definition.key(), position);
        return scopeResolver.displayName(new ScopeContext(entry.scopeType(), entry.scopeId()));
    }

    public int getLeaderboardTotalEntries(String dataKey) {
        DataDefinition definition = dataRegistry.getRequired(dataKey);
        if (!leaderboardService.supports(definition)) {
            throw new IllegalStateException("Data '" + definition.key() + "' does not support leaderboards.");
        }

        return leaderboardService.getTotalEntries(definition.key());
    }

    public void refreshDynamicLeaderboards() {
        long now = System.currentTimeMillis();
        for (DataDefinition definition : dataRegistry.all()) {
            if (!leaderboardService.supports(definition)) {
                continue;
            }
            if (!isDynamicRefreshDue(definition, now)) {
                continue;
            }
            if (definition.scopeType() != ScopeType.PLAYER) {
                continue;
            }

            List<ScopeContext> dynamicScopes = dataStore.getDynamicValueScopes(definition.key());
            if (!containsPlaceholderMarkers(definition.defaultValue()) && dynamicScopes.isEmpty()) {
                continue;
            }

            Set<ScopeContext> scopes = new LinkedHashSet<>();
            if (containsPlaceholderMarkers(definition.defaultValue())) {
                scopes.addAll(dataStore.getLeaderboardScopes(definition.key()));
            }
            scopes.addAll(dynamicScopes);

            for (ScopeContext scope : scopes) {
                refreshDynamicLeaderboardValue(definition, scope);
            }

            dynamicLeaderboardRefreshTimestamps.put(definition.key().toLowerCase(), now);
        }
    }

    public long getEnabledLeaderboardCount() {
        return dataRegistry.all().stream()
            .filter(leaderboardService::supports)
            .count();
    }

    public long getLowestDynamicRefreshMinutes() {
        return dataRegistry.all().stream()
            .map(DataDefinition::leaderboardSettings)
            .filter(LeaderboardSettings::enabled)
            .mapToLong(LeaderboardSettings::dynamicRefreshMinutes)
            .filter(minutes -> minutes > 0)
            .min()
            .orElse(0L);
    }

    private void ensureNumeric(DataDefinition definition) {
        if (definition.dataType() != DataType.NUMBER) {
            throw new IllegalStateException("Data '" + definition.key() + "' is not numeric.");
        }
    }

    private Object applyBounds(Object value, DataDefinition definition) {
        if (definition.dataType() != DataType.NUMBER) {
            return value;
        }

        double numericValue = toDouble(value);
        if (definition.minValue() != null) {
            numericValue = Math.max(numericValue, definition.minValue());
        }
        if (definition.maxValue() != null) {
            numericValue = Math.min(numericValue, definition.maxValue());
        }
        return castNumber(numericValue, definition);
    }

    private double toDouble(Object value) {
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        throw new IllegalStateException("Value is not numeric.");
    }

    private Object castNumber(double value, DataType type) {
        throw new IllegalStateException("Use decimal-aware castNumber overload.");
    }

    private Object castNumber(double value, DataDefinition definition) {
        if (definition.dataType() != DataType.NUMBER) {
            throw new IllegalStateException("Data '" + definition.key() + "' is not numeric.");
        }
        if (definition.decimal()) {
            return value;
        }
        if (Math.rint(value) != value) {
            throw new IllegalArgumentException("Data '" + definition.key() + "' requires a whole number.");
        }
        return (int) value;
    }

    private boolean containsPlaceholderMarkers(Object value) {
        if (!(value instanceof String stringValue)) {
            return false;
        }
        return placeholderValueResolver.containsPlaceholderMarkers(stringValue);
    }

    private boolean isDynamicRefreshDue(DataDefinition definition, long now) {
        int minutes = definition.leaderboardSettings().dynamicRefreshMinutes();
        if (minutes <= 0) {
            return false;
        }

        Long lastRefreshAt = dynamicLeaderboardRefreshTimestamps.get(definition.key().toLowerCase());
        if (lastRefreshAt == null) {
            return true;
        }
        return now - lastRefreshAt >= minutes * 60_000L;
    }

    public void validateDefinitions() {
        for (DataDefinition definition : dataRegistry.all()) {
            validateDefaultPresence(definition);
            validateBoundsConfiguration(definition);
            validateStaticDefaultValue(definition);
            validateDynamicLeaderboardSupport(definition, definition.defaultValue());
        }
    }

    private void validateDynamicLeaderboardSupport(DataDefinition definition, Object rawValue) {
        if (!leaderboardService.supports(definition)) {
            return;
        }
        if (definition.scopeType() == ScopeType.PLAYER) {
            return;
        }
        if (containsPlaceholderMarkers(rawValue)) {
            throw new IllegalStateException(
                "Numeric leaderboard data '" + definition.key() + "' cannot use PlaceholderAPI values outside player scope."
            );
        }
    }

    private void refreshDynamicLeaderboardValue(DataDefinition definition, ScopeContext scope) {
        OfflinePlayer player = Bukkit.getOfflinePlayer(UUID.fromString(scope.id()));
        Object boundedValue = dataStore.findValue(scope, definition.key())
            .map(value -> resolveEffectiveValue(player, definition, value))
            .orElseGet(() -> resolveEffectiveValue(player, definition, definition.defaultValue()));
        leaderboardService.update(definition, scope, boundedValue);
    }

    private Object resolveEffectiveValue(CommandSender sender, DataDefinition definition, Object rawValue) {
        return applyBounds(resolveValue(sender, definition, rawValue), definition);
    }

    private Object resolveEffectiveValue(OfflinePlayer player, DataDefinition definition, Object rawValue) {
        return applyBounds(resolveValue(player, definition, rawValue), definition);
    }

    private double resolveNumericAmount(CommandSender sender, DataDefinition definition, String rawAmount) {
        return toDouble(resolveValue(sender, definition, rawAmount));
    }

    private double resolveNumericAmount(OfflinePlayer player, DataDefinition definition, String rawAmount) {
        return toDouble(resolveValue(player, definition, rawAmount));
    }

    private Object getValue(CommandSender sender, ScopeContext scope, DataDefinition definition) {
        return dataStore.findValue(scope, definition.key())
            .map(value -> resolveEffectiveValue(sender, definition, value))
            .orElseGet(() -> resolveEffectiveValue(sender, definition, definition.defaultValue()));
    }

    private Object getValue(OfflinePlayer player, ScopeContext scope, DataDefinition definition) {
        return dataStore.findValue(scope, definition.key())
            .map(value -> resolveEffectiveValue(player, definition, value))
            .orElseGet(() -> resolveEffectiveValue(player, definition, definition.defaultValue()));
    }

    private Object getDefaultValue(CommandSender sender, DataDefinition definition, ScopeContext scope) {
        return resolveEffectiveValue(sender, definition, definition.defaultValue());
    }

    private Object getDefaultValue(OfflinePlayer player, DataDefinition definition, ScopeContext scope) {
        return resolveEffectiveValue(player, definition, definition.defaultValue());
    }

    private void validateDefaultPresence(DataDefinition definition) {
        if (definition.defaultValue() == null) {
            throw new IllegalStateException("Data '" + definition.key() + "' must define a default value.");
        }
    }

    private void validateBoundsConfiguration(DataDefinition definition) {
        boolean numeric = definition.dataType() == DataType.NUMBER;
        if (!numeric && (definition.minValue() != null || definition.maxValue() != null)) {
            throw new IllegalStateException("Data '" + definition.key() + "' cannot define min/max when it is not numeric.");
        }
        if (definition.minValue() != null && definition.maxValue() != null && definition.minValue() > definition.maxValue()) {
            throw new IllegalStateException("Data '" + definition.key() + "' has min greater than max.");
        }
    }

    private void validateStaticDefaultValue(DataDefinition definition) {
        if (containsPlaceholderMarkers(definition.defaultValue())) {
            return;
        }

        try {
            switch (definition.dataType()) {
                case STRING -> {
                    return;
                }
                case NUMBER -> resolveStaticNumericDefault(definition);
            }
        } catch (IllegalArgumentException | IllegalStateException exception) {
            throw new IllegalStateException("Data '" + definition.key() + "' has an invalid default value.", exception);
        }
    }

    private void resolveStaticNumericDefault(DataDefinition definition) {
        Object defaultValue = definition.defaultValue();
        if (defaultValue instanceof Number number) {
            castNumber(number.doubleValue(), definition);
            return;
        }
        numericExpressionResolver.resolve(String.valueOf(defaultValue).trim(), definition.decimal());
    }

    private Object resolveValue(CommandSender sender, DataDefinition definition, Object rawValue) {
        return switch (definition.dataType()) {
            case STRING -> rawValue instanceof String stringValue ? placeholderValueResolver.resolve(sender, stringValue) : rawValue;
            case NUMBER -> resolveNumericValue(sender, rawValue, definition);
        };
    }

    private Object resolveValue(OfflinePlayer player, DataDefinition definition, Object rawValue) {
        return switch (definition.dataType()) {
            case STRING -> rawValue instanceof String stringValue ? placeholderValueResolver.resolve(player, stringValue) : rawValue;
            case NUMBER -> resolveNumericValue(player, rawValue, definition);
        };
    }

    private Object resolveNumericValue(CommandSender sender, Object rawValue, DataDefinition definition) {
        if (rawValue instanceof Number number) {
            return castNumber(number.doubleValue(), definition);
        }

        String rawString = String.valueOf(rawValue).trim();
        String resolved = placeholderValueResolver.containsPlaceholderMarkers(rawString)
            ? placeholderValueResolver.resolve(sender, rawString)
            : rawString;
        return numericExpressionResolver.resolve(resolved, definition.decimal());
    }

    private Object resolveNumericValue(OfflinePlayer player, Object rawValue, DataDefinition definition) {
        if (rawValue instanceof Number number) {
            return castNumber(number.doubleValue(), definition);
        }

        String rawString = String.valueOf(rawValue).trim();
        String resolved = placeholderValueResolver.containsPlaceholderMarkers(rawString)
            ? placeholderValueResolver.resolve(player, rawString)
            : rawString;
        return numericExpressionResolver.resolve(resolved, definition.decimal());
    }

}
