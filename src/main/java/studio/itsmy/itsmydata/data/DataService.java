package studio.itsmy.itsmydata.data;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import studio.itsmy.itsmydata.leaderboard.LeaderboardEntry;
import studio.itsmy.itsmydata.leaderboard.LeaderboardPlacement;
import studio.itsmy.itsmydata.leaderboard.LeaderboardService;
import studio.itsmy.itsmydata.leaderboard.LeaderboardSettings;
import studio.itsmy.itsmydata.message.MessageService;
import studio.itsmy.itsmydata.papi.PlaceholderValueResolver;
import studio.itsmy.itsmydata.scope.ResolvedScope;
import studio.itsmy.itsmydata.scope.ScopeContext;
import studio.itsmy.itsmydata.scope.ScopeResolver;
import studio.itsmy.itsmydata.scope.ScopeType;
import studio.itsmy.itsmydata.task.TaskDispatcher;

public final class DataService {

    private final TaskDispatcher taskDispatcher;
    private final DataRegistry dataRegistry;
    private final ScopeResolver scopeResolver;
    private final DataStore dataStore;
    private final LeaderboardService leaderboardService;
    private final MessageService messageService;
    private final PlaceholderValueResolver placeholderValueResolver;
    private final NumericExpressionResolver numericExpressionResolver;
    private final ConcurrentMap<String, Long> dynamicLeaderboardRefreshTimestamps;
    private final AtomicBoolean dynamicRefreshInProgress;

    public DataService(
        TaskDispatcher taskDispatcher,
        DataRegistry dataRegistry,
        ScopeResolver scopeResolver,
        DataStore dataStore,
        LeaderboardService leaderboardService,
        MessageService messageService
    ) {
        this.taskDispatcher = taskDispatcher;
        this.dataRegistry = dataRegistry;
        this.scopeResolver = scopeResolver;
        this.dataStore = dataStore;
        this.leaderboardService = leaderboardService;
        this.messageService = messageService;
        this.placeholderValueResolver = new PlaceholderValueResolver();
        this.numericExpressionResolver = new NumericExpressionResolver();
        this.dynamicLeaderboardRefreshTimestamps = new ConcurrentHashMap<>();
        this.dynamicRefreshInProgress = new AtomicBoolean(false);
    }

    public Object getValue(OfflinePlayer player, String dataKey) {
        DataDefinition definition = dataRegistry.getRequired(dataKey);
        ScopeContext scope = scopeResolver.resolve(player, definition.scopeType());

        return dataStore.findValue(scope, definition.key())
            .map(value -> resolveEffectiveValue(player, definition, value))
            .orElseGet(() -> resolveEffectiveValue(player, definition, definition.defaultValue()));
    }

    public Object getDefaultValue(OfflinePlayer player, String dataKey) {
        DataDefinition definition = dataRegistry.getRequired(dataKey);
        return resolveEffectiveValue(player, definition, definition.defaultValue());
    }

    public CompletableFuture<Object> getValueAsync(ResolvedScope resolvedScope, DataDefinition definition) {
        return taskDispatcher.callAsync(() -> dataStore.findValue(resolvedScope.scope(), definition.key()))
            .thenCompose(storedValue -> taskDispatcher.callSync(() -> resolveStoredValue(resolvedScope.player(), definition, storedValue)));
    }

    public CompletableFuture<Object> setValueAsync(ResolvedScope resolvedScope, DataDefinition definition, Object value) {
        return taskDispatcher.callSync(() -> prepareSetValue(resolvedScope.player(), definition, value))
            .thenCompose(preparedValue -> persistValueAsync(resolvedScope.scope(), definition, preparedValue));
    }

    public CompletableFuture<Object> resetValueAsync(ResolvedScope resolvedScope, DataDefinition definition) {
        return taskDispatcher.callSync(() -> prepareResetValue(resolvedScope.player(), definition))
            .thenCompose(preparedValue -> deleteValueAsync(resolvedScope.scope(), definition, preparedValue));
    }

    public CompletableFuture<Object> giveValueAsync(ResolvedScope resolvedScope, DataDefinition definition, String rawAmount) {
        return taskDispatcher.callAsync(() -> dataStore.findValue(resolvedScope.scope(), definition.key()))
            .thenCompose(storedValue -> taskDispatcher.callSync(() -> prepareGiveValue(resolvedScope.player(), definition, rawAmount, storedValue)))
            .thenCompose(preparedValue -> persistValueAsync(resolvedScope.scope(), definition, preparedValue));
    }

    public CompletableFuture<Object> takeValueAsync(ResolvedScope resolvedScope, DataDefinition definition, String rawAmount) {
        return taskDispatcher.callAsync(() -> dataStore.findValue(resolvedScope.scope(), definition.key()))
            .thenCompose(storedValue -> taskDispatcher.callSync(() -> prepareTakeValue(resolvedScope.player(), definition, rawAmount, storedValue)))
            .thenCompose(preparedValue -> persistValueAsync(resolvedScope.scope(), definition, preparedValue));
    }

    public DataDefinition getDefinition(String dataKey) {
        return dataRegistry.getRequired(dataKey);
    }

    public List<DataDefinition> getDefinitions() {
        return dataRegistry.all().stream().toList();
    }

    public LeaderboardPlacement getLeaderboardPlacement(OfflinePlayer player, DataDefinition definition) {
        if (!leaderboardService.supports(definition)) {
            throw new IllegalStateException("Data '" + definition.key() + "' does not support leaderboards.");
        }

        ScopeContext scope = scopeResolver.resolve(player, definition.scopeType());
        return leaderboardService.getPlacement(definition, scope);
    }

    public Object getLeaderboardValue(DataDefinition definition, int position) {
        if (!leaderboardService.supports(definition)) {
            throw new IllegalStateException("Data '" + definition.key() + "' does not support leaderboards.");
        }

        if (position > getLeaderboardTotalEntries(definition)) {
            return definition.defaultValue();
        }

        LeaderboardEntry entry = leaderboardService.getEntry(definition.key(), position);
        return castNumber(entry.numericValue(), definition);
    }

    public String getLeaderboardName(DataDefinition definition, int position) {
        if (!leaderboardService.supports(definition)) {
            throw new IllegalStateException("Data '" + definition.key() + "' does not support leaderboards.");
        }

        if (position > getLeaderboardTotalEntries(definition)) {
            return messageService.raw("messages.common.none");
        }

        LeaderboardEntry entry = leaderboardService.getEntry(definition.key(), position);
        return scopeResolver.displayName(new ScopeContext(entry.scopeType(), entry.scopeId()));
    }

    public int getLeaderboardTotalEntries(DataDefinition definition) {
        if (!leaderboardService.supports(definition)) {
            throw new IllegalStateException("Data '" + definition.key() + "' does not support leaderboards.");
        }

        return leaderboardService.getTotalEntries(definition.key());
    }

    public CompletableFuture<Void> refreshDynamicLeaderboardsAsync() {
        if (!dynamicRefreshInProgress.compareAndSet(false, true)) {
            return CompletableFuture.completedFuture(null);
        }

        long now = System.currentTimeMillis();
        CompletableFuture<Void> chain = CompletableFuture.completedFuture(null);
        for (DataDefinition definition : dataRegistry.all()) {
            chain = chain.thenCompose(ignored -> refreshDynamicLeaderboardAsync(definition, now));
        }

        return chain.whenComplete((ignored, throwable) -> dynamicRefreshInProgress.set(false));
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

    public void resetDynamicRefreshState() {
        dynamicLeaderboardRefreshTimestamps.clear();
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

    private Object castNumber(double value, DataDefinition definition) {
        if (definition.decimal()) {
            return value;
        }
        if (Math.rint(value) != value) {
            throw new IllegalArgumentException("Data '" + definition.key() + "' requires a whole number.");
        }
        return (int) value;
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
        if (placeholderValueResolver.containsPlaceholderMarkers(rawValue)) {
            throw new IllegalStateException(
                "Numeric leaderboard data '" + definition.key() + "' cannot use PlaceholderAPI values outside player scope."
            );
        }
    }

    private CompletableFuture<Void> refreshDynamicLeaderboardAsync(DataDefinition definition, long now) {
        if (!leaderboardService.supports(definition) || !isDynamicRefreshDue(definition, now) || definition.scopeType() != ScopeType.PLAYER) {
            return CompletableFuture.completedFuture(null);
        }

        return taskDispatcher.callAsync(() -> loadDynamicRefreshScopes(definition))
            .thenCompose(scopes -> {
                CompletableFuture<Void> chain = CompletableFuture.completedFuture(null);
                for (ScopeContext scope : scopes) {
                    chain = chain.thenCompose(ignored -> refreshDynamicLeaderboardValueAsync(definition, scope));
                }
                return chain.thenRun(() -> dynamicLeaderboardRefreshTimestamps.put(definition.key().toLowerCase(), now));
            });
    }

    private Set<ScopeContext> loadDynamicRefreshScopes(DataDefinition definition) {
        List<ScopeContext> dynamicScopes = dataStore.getDynamicValueScopes(definition.key());
        if (!placeholderValueResolver.containsPlaceholderMarkers(definition.defaultValue()) && dynamicScopes.isEmpty()) {
            return Set.of();
        }

        Set<ScopeContext> scopes = new LinkedHashSet<>();
        if (placeholderValueResolver.containsPlaceholderMarkers(definition.defaultValue())) {
            scopes.addAll(dataStore.getLeaderboardScopes(definition.key()));
        }
        scopes.addAll(dynamicScopes);
        return scopes;
    }

    private CompletableFuture<Void> refreshDynamicLeaderboardValueAsync(DataDefinition definition, ScopeContext scope) {
        return taskDispatcher.callAsync(() -> dataStore.findValue(scope, definition.key()))
            .thenCompose(storedValue -> taskDispatcher.callSync(() -> {
                OfflinePlayer player = Bukkit.getOfflinePlayer(UUID.fromString(scope.id()));
                return resolveStoredValue(player, definition, storedValue);
            }))
            .thenCompose(boundedValue -> taskDispatcher.callAsync(() -> {
                leaderboardService.update(definition, scope, boundedValue);
                return null;
            }));
    }

    private Object resolveEffectiveValue(OfflinePlayer player, DataDefinition definition, Object rawValue) {
        return switch (definition.dataType()) {
            case STRING -> resolveStringValue(player, rawValue);
            case NUMBER -> applyBounds(resolveNumericValue(player, rawValue, definition), definition);
        };
    }

    private PreparedValue prepareSetValue(OfflinePlayer player, DataDefinition definition, Object value) {
        Object parsedValue = resolveEffectiveValue(player, definition, value);
        validateDynamicLeaderboardSupport(definition, value);
        return toPreparedValue(definition, parsedValue);
    }

    private PreparedValue prepareResetValue(OfflinePlayer player, DataDefinition definition) {
        return toPreparedValue(definition, resolveEffectiveValue(player, definition, definition.defaultValue()));
    }

    private PreparedValue prepareGiveValue(OfflinePlayer player, DataDefinition definition, String rawAmount, Optional<String> storedValue) {
        return prepareNumericDeltaValue(player, definition, rawAmount, storedValue, 1D);
    }

    private PreparedValue prepareTakeValue(OfflinePlayer player, DataDefinition definition, String rawAmount, Optional<String> storedValue) {
        return prepareNumericDeltaValue(player, definition, rawAmount, storedValue, -1D);
    }

    private PreparedValue prepareNumericDeltaValue(
        OfflinePlayer player,
        DataDefinition definition,
        String rawAmount,
        Optional<String> storedValue,
        double direction
    ) {
        if (definition.dataType() != DataType.NUMBER) {
            throw new IllegalStateException("Data '" + definition.key() + "' is not numeric.");
        }

        Object currentValue = resolveStoredValue(player, definition, storedValue);
        double nextValue = toDouble(currentValue) + (toDouble(resolveEffectiveValue(player, definition, rawAmount)) * direction);
        Object boundedValue = applyBounds(castNumber(nextValue, definition), definition);
        return toPreparedValue(definition, boundedValue);
    }

    private CompletableFuture<Object> persistValueAsync(ScopeContext scope, DataDefinition definition, PreparedValue preparedValue) {
        return taskDispatcher.callAsync(() -> {
            dataStore.saveValueAndSyncLeaderboard(
                scope,
                definition.key(),
                String.valueOf(preparedValue.value()),
                preparedValue.leaderboardValue()
            );
            return preparedValue.value();
        });
    }

    private CompletableFuture<Object> deleteValueAsync(ScopeContext scope, DataDefinition definition, PreparedValue preparedValue) {
        return taskDispatcher.callAsync(() -> {
            dataStore.deleteValueAndSyncLeaderboard(scope, definition.key(), preparedValue.leaderboardValue());
            return preparedValue.value();
        });
    }

    private PreparedValue toPreparedValue(DataDefinition definition, Object value) {
        Double leaderboardValue = leaderboardService.supports(definition) ? toDouble(value) : null;
        return new PreparedValue(value, leaderboardValue);
    }

    private Object resolveStoredValue(OfflinePlayer player, DataDefinition definition, Optional<String> storedValue) {
        return storedValue
            .map(value -> resolveEffectiveValue(player, definition, value))
            .orElseGet(() -> resolveEffectiveValue(player, definition, definition.defaultValue()));
    }

    private Object resolveStringValue(OfflinePlayer player, Object rawValue) {
        if (!(rawValue instanceof String stringValue)) {
            return rawValue;
        }
        return player == null ? stringValue : placeholderValueResolver.resolve(player, stringValue);
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

    private record PreparedValue(Object value, Double leaderboardValue) {
    }

}
