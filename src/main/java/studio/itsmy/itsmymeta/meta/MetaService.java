package studio.itsmy.itsmymeta.meta;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import studio.itsmy.itsmymeta.leaderboard.LeaderboardEntry;
import studio.itsmy.itsmymeta.leaderboard.LeaderboardPlacement;
import studio.itsmy.itsmymeta.leaderboard.LeaderboardService;
import studio.itsmy.itsmymeta.leaderboard.LeaderboardSettings;
import studio.itsmy.itsmymeta.papi.PlaceholderValueResolver;
import studio.itsmy.itsmymeta.scope.ScopeContext;
import studio.itsmy.itsmymeta.scope.ScopeResolver;
import studio.itsmy.itsmymeta.scope.ScopeType;

public final class MetaService {

    private final MetaRegistry metaRegistry;
    private final ScopeResolver scopeResolver;
    private final MetaStore metaStore;
    private final LeaderboardService leaderboardService;
    private final MetaValueParser valueParser;
    private final PlaceholderValueResolver placeholderValueResolver;
    private final NumericExpressionResolver numericExpressionResolver;
    private final ConcurrentMap<String, Long> dynamicLeaderboardRefreshTimestamps;

    public MetaService(MetaRegistry metaRegistry, ScopeResolver scopeResolver, MetaStore metaStore, LeaderboardService leaderboardService) {
        this.metaRegistry = metaRegistry;
        this.scopeResolver = scopeResolver;
        this.metaStore = metaStore;
        this.leaderboardService = leaderboardService;
        this.valueParser = new MetaValueParser();
        this.placeholderValueResolver = new PlaceholderValueResolver();
        this.numericExpressionResolver = new NumericExpressionResolver();
        this.dynamicLeaderboardRefreshTimestamps = new ConcurrentHashMap<>();
    }

    public Object getValue(CommandSender sender, String metaKey) {
        MetaDefinition definition = metaRegistry.getRequired(metaKey);
        ScopeContext scope = scopeResolver.resolve(sender, definition.scopeType());
        return getValue(sender, scope, definition);
    }

    public Object getValue(OfflinePlayer player, String metaKey) {
        MetaDefinition definition = metaRegistry.getRequired(metaKey);
        ScopeContext scope = scopeResolver.resolve(player, definition.scopeType());
        return getValue(player, scope, definition);
    }

    public Object getValue(CommandSender sender, ScopeContext scope, String metaKey) {
        return getValue(sender, scope, metaRegistry.getRequired(metaKey));
    }

    public Object getValue(OfflinePlayer player, ScopeContext scope, String metaKey) {
        return getValue(player, scope, metaRegistry.getRequired(metaKey));
    }

    public Object getDefaultValue(CommandSender sender, String metaKey) {
        MetaDefinition definition = metaRegistry.getRequired(metaKey);
        return getDefaultValue(sender, definition, definition.scopeType() == ScopeType.GLOBAL ? new ScopeContext(ScopeType.GLOBAL, "global") : scopeResolver.resolve(sender, definition.scopeType()));
    }

    public Object getDefaultValue(OfflinePlayer player, String metaKey) {
        MetaDefinition definition = metaRegistry.getRequired(metaKey);
        return getDefaultValue(player, metaKey, scopeResolver.resolve(player, definition.scopeType()));
    }

    public Object getDefaultValue(CommandSender sender, String metaKey, ScopeContext scope) {
        return getDefaultValue(sender, metaRegistry.getRequired(metaKey), scope);
    }

    public Object getDefaultValue(OfflinePlayer player, String metaKey, ScopeContext scope) {
        return getDefaultValue(player, metaRegistry.getRequired(metaKey), scope);
    }

    public Object setValue(CommandSender sender, String metaKey, Object value) {
        MetaDefinition definition = metaRegistry.getRequired(metaKey);
        ScopeContext scope = scopeResolver.resolve(sender, definition.scopeType());
        return setValue(sender, scope, definition, value);
    }

    public Object setValue(CommandSender sender, ScopeContext scope, String metaKey, Object value) {
        return setValue(sender, scope, metaRegistry.getRequired(metaKey), value);
    }

    public Object setValue(OfflinePlayer player, ScopeContext scope, String metaKey, Object value) {
        return setValue(player, scope, metaRegistry.getRequired(metaKey), value);
    }

    public Object resetValue(CommandSender sender, String metaKey) {
        MetaDefinition definition = metaRegistry.getRequired(metaKey);
        ScopeContext scope = scopeResolver.resolve(sender, definition.scopeType());
        return resetValue(sender, scope, definition);
    }

    public Object resetValue(CommandSender sender, ScopeContext scope, String metaKey) {
        return resetValue(sender, scope, metaRegistry.getRequired(metaKey));
    }

    public Object resetValue(OfflinePlayer player, ScopeContext scope, String metaKey) {
        return resetValue(player, scope, metaRegistry.getRequired(metaKey));
    }

    public Object giveValue(CommandSender sender, String metaKey, String rawAmount) {
        MetaDefinition definition = metaRegistry.getRequired(metaKey);
        ScopeContext scope = scopeResolver.resolve(sender, definition.scopeType());
        return giveValue(sender, scope, definition, rawAmount);
    }

    public Object giveValue(CommandSender sender, ScopeContext scope, String metaKey, String rawAmount) {
        return giveValue(sender, scope, metaRegistry.getRequired(metaKey), rawAmount);
    }

    public Object giveValue(OfflinePlayer player, ScopeContext scope, String metaKey, String rawAmount) {
        return giveValue(player, scope, metaRegistry.getRequired(metaKey), rawAmount);
    }

    public Object takeValue(CommandSender sender, String metaKey, String rawAmount) {
        MetaDefinition definition = metaRegistry.getRequired(metaKey);
        ScopeContext scope = scopeResolver.resolve(sender, definition.scopeType());
        return takeValue(sender, scope, definition, rawAmount);
    }

    public Object takeValue(CommandSender sender, ScopeContext scope, String metaKey, String rawAmount) {
        return takeValue(sender, scope, metaRegistry.getRequired(metaKey), rawAmount);
    }

    public Object takeValue(OfflinePlayer player, ScopeContext scope, String metaKey, String rawAmount) {
        return takeValue(player, scope, metaRegistry.getRequired(metaKey), rawAmount);
    }

    private Object setValue(CommandSender sender, ScopeContext scope, MetaDefinition definition, Object value) {
        Object parsedValue = resolveEffectiveValue(sender, definition, value);
        validateDynamicLeaderboardSupport(definition, value);
        Double leaderboardValue = leaderboardService.supports(definition) ? toDouble(parsedValue) : null;
        metaStore.saveValueAndSyncLeaderboard(scope, definition.key(), valueParser.serialize(parsedValue), leaderboardValue);
        return parsedValue;
    }

    private Object setValue(OfflinePlayer player, ScopeContext scope, MetaDefinition definition, Object value) {
        Object parsedValue = resolveEffectiveValue(player, definition, value);
        validateDynamicLeaderboardSupport(definition, value);
        Double leaderboardValue = leaderboardService.supports(definition) ? toDouble(parsedValue) : null;
        metaStore.saveValueAndSyncLeaderboard(scope, definition.key(), valueParser.serialize(parsedValue), leaderboardValue);
        return parsedValue;
    }

    private Object resetValue(CommandSender sender, ScopeContext scope, MetaDefinition definition) {
        Object resolvedDefault = resolveEffectiveValue(sender, definition, definition.defaultValue());
        Double leaderboardValue = leaderboardService.supports(definition) ? toDouble(resolvedDefault) : null;
        metaStore.deleteValueAndSyncLeaderboard(scope, definition.key(), leaderboardValue);
        return resolvedDefault;
    }

    private Object resetValue(OfflinePlayer player, ScopeContext scope, MetaDefinition definition) {
        Object resolvedDefault = resolveEffectiveValue(player, definition, definition.defaultValue());
        Double leaderboardValue = leaderboardService.supports(definition) ? toDouble(resolvedDefault) : null;
        metaStore.deleteValueAndSyncLeaderboard(scope, definition.key(), leaderboardValue);
        return resolvedDefault;
    }

    private Object giveValue(CommandSender sender, ScopeContext scope, MetaDefinition definition, String rawAmount) {
        ensureNumeric(definition);
        Object currentValue = getValue(sender, scope, definition);
        double nextValue = toDouble(currentValue) + resolveNumericAmount(sender, definition, rawAmount);
        Object typedValue = castNumber(nextValue, definition);
        Object boundedValue = applyBounds(typedValue, definition);
        metaStore.saveValueAndSyncLeaderboard(scope, definition.key(), valueParser.serialize(boundedValue), toDouble(boundedValue));
        return boundedValue;
    }

    private Object giveValue(OfflinePlayer player, ScopeContext scope, MetaDefinition definition, String rawAmount) {
        ensureNumeric(definition);
        Object currentValue = getValue(player, scope, definition);
        double nextValue = toDouble(currentValue) + resolveNumericAmount(player, definition, rawAmount);
        Object typedValue = castNumber(nextValue, definition);
        Object boundedValue = applyBounds(typedValue, definition);
        metaStore.saveValueAndSyncLeaderboard(scope, definition.key(), valueParser.serialize(boundedValue), toDouble(boundedValue));
        return boundedValue;
    }

    private Object takeValue(CommandSender sender, ScopeContext scope, MetaDefinition definition, String rawAmount) {
        ensureNumeric(definition);
        Object currentValue = getValue(sender, scope, definition);
        double nextValue = toDouble(currentValue) - resolveNumericAmount(sender, definition, rawAmount);
        Object typedValue = castNumber(nextValue, definition);
        Object boundedValue = applyBounds(typedValue, definition);
        metaStore.saveValueAndSyncLeaderboard(scope, definition.key(), valueParser.serialize(boundedValue), toDouble(boundedValue));
        return boundedValue;
    }

    private Object takeValue(OfflinePlayer player, ScopeContext scope, MetaDefinition definition, String rawAmount) {
        ensureNumeric(definition);
        Object currentValue = getValue(player, scope, definition);
        double nextValue = toDouble(currentValue) - resolveNumericAmount(player, definition, rawAmount);
        Object typedValue = castNumber(nextValue, definition);
        Object boundedValue = applyBounds(typedValue, definition);
        metaStore.saveValueAndSyncLeaderboard(scope, definition.key(), valueParser.serialize(boundedValue), toDouble(boundedValue));
        return boundedValue;
    }

    public MetaDefinition getDefinition(String metaKey) {
        return metaRegistry.getRequired(metaKey);
    }

    public List<MetaDefinition> getDefinitions() {
        return metaRegistry.all().stream().toList();
    }

    public List<String> getSupportedScopes() {
        return List.of(
            ScopeType.GLOBAL.name().toLowerCase(),
            ScopeType.PLAYER.name().toLowerCase(),
            ScopeType.WORLD.name().toLowerCase(),
            ScopeType.SUPERIORSKYBLOCK.name().toLowerCase()
        );
    }

    public LeaderboardPlacement getLeaderboardPlacement(CommandSender sender, String metaKey) {
        MetaDefinition definition = metaRegistry.getRequired(metaKey);
        if (!leaderboardService.supports(definition)) {
            throw new IllegalStateException("Meta '" + definition.key() + "' does not support leaderboards.");
        }

        ScopeContext scope = scopeResolver.resolve(sender, definition.scopeType());
        return leaderboardService.getPlacement(definition, scope);
    }

    public LeaderboardPlacement getLeaderboardPlacement(OfflinePlayer player, String metaKey) {
        MetaDefinition definition = metaRegistry.getRequired(metaKey);
        if (!leaderboardService.supports(definition)) {
            throw new IllegalStateException("Meta '" + definition.key() + "' does not support leaderboards.");
        }

        ScopeContext scope = scopeResolver.resolve(player, definition.scopeType());
        return leaderboardService.getPlacement(definition, scope);
    }

    public Object getLeaderboardValue(String metaKey, int position) {
        MetaDefinition definition = metaRegistry.getRequired(metaKey);
        if (!leaderboardService.supports(definition)) {
            throw new IllegalStateException("Meta '" + definition.key() + "' does not support leaderboards.");
        }

        LeaderboardEntry entry = leaderboardService.getEntry(definition.key(), position);
        return castNumber(entry.numericValue(), definition);
    }

    public String getLeaderboardName(String metaKey, int position) {
        MetaDefinition definition = metaRegistry.getRequired(metaKey);
        if (!leaderboardService.supports(definition)) {
            throw new IllegalStateException("Meta '" + definition.key() + "' does not support leaderboards.");
        }

        LeaderboardEntry entry = leaderboardService.getEntry(definition.key(), position);
        return scopeResolver.displayName(new ScopeContext(entry.scopeType(), entry.scopeId()));
    }

    public int getLeaderboardTotalEntries(String metaKey) {
        MetaDefinition definition = metaRegistry.getRequired(metaKey);
        if (!leaderboardService.supports(definition)) {
            throw new IllegalStateException("Meta '" + definition.key() + "' does not support leaderboards.");
        }

        return leaderboardService.getTotalEntries(definition.key());
    }

    public void refreshDynamicLeaderboards() {
        long now = System.currentTimeMillis();
        for (MetaDefinition definition : metaRegistry.all()) {
            if (!leaderboardService.supports(definition)) {
                continue;
            }
            if (!isDynamicRefreshDue(definition, now)) {
                continue;
            }
            if (definition.scopeType() != ScopeType.PLAYER) {
                continue;
            }

            List<ScopeContext> dynamicScopes = metaStore.getDynamicValueScopes(definition.key());
            if (!containsPlaceholderMarkers(definition.defaultValue()) && dynamicScopes.isEmpty()) {
                continue;
            }

            Set<ScopeContext> scopes = new LinkedHashSet<>();
            if (containsPlaceholderMarkers(definition.defaultValue())) {
                scopes.addAll(metaStore.getLeaderboardScopes(definition.key()));
            }
            scopes.addAll(dynamicScopes);

            for (ScopeContext scope : scopes) {
                refreshDynamicLeaderboardValue(definition, scope);
            }

            dynamicLeaderboardRefreshTimestamps.put(definition.key().toLowerCase(), now);
        }
    }

    public long getEnabledLeaderboardCount() {
        return metaRegistry.all().stream()
            .filter(leaderboardService::supports)
            .count();
    }

    public long getLowestDynamicRefreshMinutes() {
        return metaRegistry.all().stream()
            .map(MetaDefinition::leaderboardSettings)
            .filter(LeaderboardSettings::enabled)
            .mapToLong(LeaderboardSettings::dynamicRefreshMinutes)
            .filter(minutes -> minutes > 0)
            .min()
            .orElse(0L);
    }

    private void ensureNumeric(MetaDefinition definition) {
        if (definition.metaType() != MetaType.NUMBER) {
            throw new IllegalStateException("Meta '" + definition.key() + "' is not numeric.");
        }
    }

    private Object applyBounds(Object value, MetaDefinition definition) {
        if (definition.metaType() != MetaType.NUMBER) {
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

    private Object castNumber(double value, MetaType type) {
        throw new IllegalStateException("Use decimal-aware castNumber overload.");
    }

    private Object castNumber(double value, MetaDefinition definition) {
        if (definition.metaType() != MetaType.NUMBER) {
            throw new IllegalStateException("Meta '" + definition.key() + "' is not numeric.");
        }
        if (definition.decimal()) {
            return value;
        }
        if (Math.rint(value) != value) {
            throw new IllegalArgumentException("Meta '" + definition.key() + "' requires a whole number.");
        }
        return (int) value;
    }

    private boolean containsPlaceholderMarkers(Object value) {
        if (!(value instanceof String stringValue)) {
            return false;
        }
        return placeholderValueResolver.containsPlaceholderMarkers(stringValue);
    }

    private boolean isDynamicRefreshDue(MetaDefinition definition, long now) {
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
        for (MetaDefinition definition : metaRegistry.all()) {
            validateDefaultPresence(definition);
            validateBoundsConfiguration(definition);
            validateStaticDefaultValue(definition);
            validateDynamicLeaderboardSupport(definition, definition.defaultValue());
        }
    }

    private void validateDynamicLeaderboardSupport(MetaDefinition definition, Object rawValue) {
        if (!leaderboardService.supports(definition)) {
            return;
        }
        if (definition.scopeType() == ScopeType.PLAYER) {
            return;
        }
        if (containsPlaceholderMarkers(rawValue)) {
            throw new IllegalStateException(
                "Numeric leaderboard meta '" + definition.key() + "' cannot use PlaceholderAPI values outside player scope."
            );
        }
    }

    private void refreshDynamicLeaderboardValue(MetaDefinition definition, ScopeContext scope) {
        OfflinePlayer player = Bukkit.getOfflinePlayer(UUID.fromString(scope.id()));
        Object boundedValue = metaStore.findValue(scope, definition.key())
            .map(value -> resolveEffectiveValue(player, definition, value))
            .orElseGet(() -> resolveEffectiveValue(player, definition, definition.defaultValue()));
        leaderboardService.update(definition, scope, boundedValue);
    }

    private Object resolveEffectiveValue(CommandSender sender, MetaDefinition definition, Object rawValue) {
        return applyBounds(resolveValue(sender, definition, rawValue), definition);
    }

    private Object resolveEffectiveValue(OfflinePlayer player, MetaDefinition definition, Object rawValue) {
        return applyBounds(resolveValue(player, definition, rawValue), definition);
    }

    private double resolveNumericAmount(CommandSender sender, MetaDefinition definition, String rawAmount) {
        return toDouble(resolveValue(sender, definition, rawAmount));
    }

    private double resolveNumericAmount(OfflinePlayer player, MetaDefinition definition, String rawAmount) {
        return toDouble(resolveValue(player, definition, rawAmount));
    }

    private Object getValue(CommandSender sender, ScopeContext scope, MetaDefinition definition) {
        return metaStore.findValue(scope, definition.key())
            .map(value -> resolveEffectiveValue(sender, definition, value))
            .orElseGet(() -> resolveEffectiveValue(sender, definition, definition.defaultValue()));
    }

    private Object getValue(OfflinePlayer player, ScopeContext scope, MetaDefinition definition) {
        return metaStore.findValue(scope, definition.key())
            .map(value -> resolveEffectiveValue(player, definition, value))
            .orElseGet(() -> resolveEffectiveValue(player, definition, definition.defaultValue()));
    }

    private Object getDefaultValue(CommandSender sender, MetaDefinition definition, ScopeContext scope) {
        return resolveEffectiveValue(sender, definition, definition.defaultValue());
    }

    private Object getDefaultValue(OfflinePlayer player, MetaDefinition definition, ScopeContext scope) {
        return resolveEffectiveValue(player, definition, definition.defaultValue());
    }

    private void validateDefaultPresence(MetaDefinition definition) {
        if (definition.defaultValue() == null) {
            throw new IllegalStateException("Meta '" + definition.key() + "' must define a default value.");
        }
    }

    private void validateBoundsConfiguration(MetaDefinition definition) {
        boolean numeric = definition.metaType() == MetaType.NUMBER;
        if (!numeric && (definition.minValue() != null || definition.maxValue() != null)) {
            throw new IllegalStateException("Meta '" + definition.key() + "' cannot define min/max when it is not numeric.");
        }
        if (definition.minValue() != null && definition.maxValue() != null && definition.minValue() > definition.maxValue()) {
            throw new IllegalStateException("Meta '" + definition.key() + "' has min greater than max.");
        }
    }

    private void validateStaticDefaultValue(MetaDefinition definition) {
        if (containsPlaceholderMarkers(definition.defaultValue())) {
            return;
        }

        try {
            switch (definition.metaType()) {
                case STRING -> {
                    return;
                }
                case NUMBER -> resolveStaticNumericDefault(definition);
            }
        } catch (IllegalArgumentException | IllegalStateException exception) {
            throw new IllegalStateException("Meta '" + definition.key() + "' has an invalid default value.", exception);
        }
    }

    private void resolveStaticNumericDefault(MetaDefinition definition) {
        Object defaultValue = definition.defaultValue();
        if (defaultValue instanceof Number number) {
            castNumber(number.doubleValue(), definition);
            return;
        }
        numericExpressionResolver.resolve(String.valueOf(defaultValue).trim(), definition.decimal());
    }

    private Object resolveValue(CommandSender sender, MetaDefinition definition, Object rawValue) {
        return switch (definition.metaType()) {
            case STRING -> rawValue instanceof String stringValue ? placeholderValueResolver.resolve(sender, stringValue) : rawValue;
            case NUMBER -> resolveNumericValue(sender, rawValue, definition);
        };
    }

    private Object resolveValue(OfflinePlayer player, MetaDefinition definition, Object rawValue) {
        return switch (definition.metaType()) {
            case STRING -> rawValue instanceof String stringValue ? placeholderValueResolver.resolve(player, stringValue) : rawValue;
            case NUMBER -> resolveNumericValue(player, rawValue, definition);
        };
    }

    private Object resolveNumericValue(CommandSender sender, Object rawValue, MetaDefinition definition) {
        if (rawValue instanceof Number number) {
            return castNumber(number.doubleValue(), definition);
        }

        String rawString = String.valueOf(rawValue).trim();
        String resolved = placeholderValueResolver.containsPlaceholderMarkers(rawString)
            ? placeholderValueResolver.resolve(sender, rawString)
            : rawString;
        return numericExpressionResolver.resolve(resolved, definition.decimal());
    }

    private Object resolveNumericValue(OfflinePlayer player, Object rawValue, MetaDefinition definition) {
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
