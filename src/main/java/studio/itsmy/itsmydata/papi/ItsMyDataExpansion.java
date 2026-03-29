package studio.itsmy.itsmydata.papi;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Map;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import studio.itsmy.itsmydata.ItsMyDataPlugin;
import studio.itsmy.itsmydata.leaderboard.LeaderboardPlacement;
import studio.itsmy.itsmydata.data.DataDefinition;
import studio.itsmy.itsmydata.data.DataType;
import studio.itsmy.itsmydata.data.DataService;

public final class ItsMyDataExpansion extends PlaceholderExpansion {

    private static final String[] SUPPORTED_SUFFIXES = {"_fixed", "_formatted", "_commas", "_default", "_min", "_max"};
    private static final Pattern LEADERBOARD_VALUE_PATTERN = Pattern.compile("(.+)_leaderboard_(\\d+)(?:_(fixed|formatted|commas|name))?");
    private static final Pattern LEADERBOARD_POSITION_PATTERN = Pattern.compile("(.+)_leaderboard_position(?:_(percent))?");
    private static final long LEADERBOARD_CACHE_TTL_MILLIS = 3_000L;
    private static final ThreadLocal<DecimalFormat> FIXED_FORMAT = ThreadLocal.withInitial(
        () -> new DecimalFormat("0.00", DecimalFormatSymbols.getInstance(Locale.US))
    );
    private static final ThreadLocal<DecimalFormat> COMMA_FORMAT = ThreadLocal.withInitial(
        () -> new DecimalFormat("#,##0.##", DecimalFormatSymbols.getInstance(Locale.US))
    );
    private static final ThreadLocal<DecimalFormat> STRIP_FORMAT = ThreadLocal.withInitial(
        () -> new DecimalFormat("0.##", DecimalFormatSymbols.getInstance(Locale.US))
    );

    private final ItsMyDataPlugin plugin;
    private final DataService dataService;
    private final Map<String, CachedValue<String>> leaderboardValueCache;
    private final Map<String, CachedValue<String>> leaderboardPositionCache;
    private final Map<String, CachedValue<String>> leaderboardTotalPlayersCache;
    private final Set<String> loggedErrors;

    public ItsMyDataExpansion(ItsMyDataPlugin plugin, DataService dataService) {
        this.plugin = plugin;
        this.dataService = dataService;
        this.leaderboardValueCache = new ConcurrentHashMap<>();
        this.leaderboardPositionCache = new ConcurrentHashMap<>();
        this.leaderboardTotalPlayersCache = new ConcurrentHashMap<>();
        this.loggedErrors = ConcurrentHashMap.newKeySet();
    }

    @Override
    public @NotNull String getIdentifier() {
        return "itsmydata";
    }

    @Override
    public @NotNull String getAuthor() {
        return "ItsMyDesktop";
    }

    @Override
    public @NotNull String getVersion() {
        return plugin.getPluginData().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public @Nullable String onRequest(OfflinePlayer player, @NotNull String params) {
        if (params.isBlank()) {
            return null;
        }

        try {
            String leaderboardValue = resolveLeaderboardValuePlaceholder(player, params);
            if (leaderboardValue != null) {
                return leaderboardValue;
            }

            String leaderboardPosition = resolveLeaderboardPositionPlaceholder(player, params);
            if (leaderboardPosition != null) {
                return leaderboardPosition;
            }

            String leaderboardTotalPlayers = resolveLeaderboardTotalPlayersPlaceholder(params);
            if (leaderboardTotalPlayers != null) {
                return leaderboardTotalPlayers;
            }

            DataDefinition definition = resolveDefinition(params);
            ParsedPlaceholder placeholder = parsePlaceholder(params, definition.key());

            return switch (placeholder.suffix()) {
                case "_default" -> String.valueOf(dataService.getDefaultValue(player, definition.key()));
                case "_min" -> formatBound(definition.minValue());
                case "_max" -> formatBound(definition.maxValue());
                case "_fixed" -> formatFixed(dataService.getValue(player, definition.key()), definition.dataType());
                case "_formatted" -> formatCompact(dataService.getValue(player, definition.key()), definition.dataType());
                case "_commas" -> formatWithCommas(dataService.getValue(player, definition.key()), definition.dataType());
                default -> String.valueOf(dataService.getValue(player, definition.key()));
            };
        } catch (IllegalArgumentException | IllegalStateException exception) {
            logPlaceholderFailure(params, exception);
            return null;
        }
    }

    private String resolveLeaderboardValuePlaceholder(OfflinePlayer player, String params) {
        Matcher matcher = LEADERBOARD_VALUE_PATTERN.matcher(params);
        if (!matcher.matches()) {
            return null;
        }

        String dataKey = matcher.group(1);
        int position = Integer.parseInt(matcher.group(2));
        String format = matcher.group(3);

        return getCachedValue(leaderboardValueCache, params, () -> {
            DataDefinition definition = dataService.getDefinition(dataKey);
            if (position > dataService.getLeaderboardTotalEntries(dataKey)) {
                return fallbackLeaderboardValue(player, definition, format);
            }
            if ("name".equals(format)) {
                return dataService.getLeaderboardName(dataKey, position);
            }
            Object value = dataService.getLeaderboardValue(dataKey, position);

            if ("fixed".equals(format)) {
                return formatFixed(value, definition.dataType());
            }
            if ("formatted".equals(format)) {
                return formatCompact(value, definition.dataType());
            }
            if ("commas".equals(format)) {
                return formatWithCommas(value, definition.dataType());
            }
            return String.valueOf(value);
        });
    }

    private String fallbackLeaderboardValue(OfflinePlayer player, DataDefinition definition, String format) {
        if ("name".equals(format)) {
            return plugin.getMessageService().raw("messages.common.none");
        }

        Object defaultValue = player != null
            ? dataService.getDefaultValue(player, definition.key())
            : definition.defaultValue();

        if ("fixed".equals(format)) {
            return formatFixed(defaultValue, definition.dataType());
        }
        if ("formatted".equals(format)) {
            return formatCompact(defaultValue, definition.dataType());
        }
        if ("commas".equals(format)) {
            return formatWithCommas(defaultValue, definition.dataType());
        }
        return String.valueOf(defaultValue);
    }

    private String resolveLeaderboardPositionPlaceholder(OfflinePlayer player, String params) {
        Matcher matcher = LEADERBOARD_POSITION_PATTERN.matcher(params);
        if (!matcher.matches()) {
            return null;
        }
        if (player == null) {
            return null;
        }

        String dataKey = matcher.group(1);
        String mode = matcher.group(2);
        String cacheKey = dataKey + "|" + player.getUniqueId() + "|" + (mode == null ? "rank" : mode);
        return getCachedValue(leaderboardPositionCache, cacheKey, () -> {
            LeaderboardPlacement placement = dataService.getLeaderboardPlacement(player, dataKey);
            return "percent".equals(mode) ? placement.displayPercent() : placement.displayRank();
        });
    }

    private String resolveLeaderboardTotalPlayersPlaceholder(String params) {
        if (!params.endsWith("_leaderboard_total_players")) {
            return null;
        }

        String dataKey = params.substring(0, params.length() - "_leaderboard_total_players".length());
        return getCachedValue(
            leaderboardTotalPlayersCache,
            dataKey,
            () -> String.valueOf(dataService.getLeaderboardTotalEntries(dataKey))
        );
    }

    private DataDefinition resolveDefinition(String params) {
        try {
            return dataService.getDefinition(params);
        } catch (IllegalArgumentException ignored) {
            ParsedPlaceholder placeholder = parsePlaceholder(params, null);
            return dataService.getDefinition(placeholder.dataKey());
        }
    }

    private ParsedPlaceholder parsePlaceholder(String params, String exactDataKey) {
        if (exactDataKey != null && exactDataKey.equalsIgnoreCase(params)) {
            return new ParsedPlaceholder(params, "");
        }

        for (String suffix : SUPPORTED_SUFFIXES) {
            if (params.endsWith(suffix) && params.length() > suffix.length()) {
                String dataKey = params.substring(0, params.length() - suffix.length());
                return new ParsedPlaceholder(dataKey, suffix);
            }
        }
        return new ParsedPlaceholder(params, "");
    }

    private String formatBound(Double bound) {
        return bound == null ? "" : stripTrailingZeros(bound);
    }

    private String formatFixed(Object value, DataType dataType) {
        if (!isNumeric(dataType)) {
            return String.valueOf(value);
        }

        return FIXED_FORMAT.get().format(asDouble(value));
    }

    private String formatWithCommas(Object value, DataType dataType) {
        if (!isNumeric(dataType)) {
            return String.valueOf(value);
        }

        return COMMA_FORMAT.get().format(asDouble(value));
    }

    private String formatCompact(Object value, DataType dataType) {
        if (!isNumeric(dataType)) {
            return String.valueOf(value);
        }

        double number = asDouble(value);
        double absolute = Math.abs(number);

        if (absolute >= 1_000_000_000) {
            return stripTrailingZeros(number / 1_000_000_000D) + "B";
        }
        if (absolute >= 1_000_000) {
            return stripTrailingZeros(number / 1_000_000D) + "M";
        }
        if (absolute >= 1_000) {
            return stripTrailingZeros(number / 1_000D) + "K";
        }
        return stripTrailingZeros(number);
    }

    private boolean isNumeric(DataType dataType) {
        return dataType == DataType.NUMBER;
    }

    private double asDouble(Object value) {
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        throw new IllegalArgumentException("Data value is not numeric.");
    }

    private String stripTrailingZeros(double value) {
        return STRIP_FORMAT.get().format(value);
    }

    private String getCachedValue(Map<String, CachedValue<String>> cache, String key, ValueSupplier supplier) {
        CachedValue<String> cachedValue = cache.get(key);
        long now = System.currentTimeMillis();
        if (cachedValue != null && cachedValue.expiresAt() > now) {
            return cachedValue.value();
        }

        String value = supplier.get();
        cache.put(key, new CachedValue<>(value, now + LEADERBOARD_CACHE_TTL_MILLIS));
        return value;
    }

    private void logPlaceholderFailure(String params, RuntimeException exception) {
        String errorKey = params + "|" + exception.getClass().getName() + "|" + exception.getMessage();
        if (loggedErrors.add(errorKey)) {
            plugin.getLogger().fine("Failed to resolve PlaceholderAPI placeholder '" + params + "': " + exception.getMessage());
        }
    }

    @FunctionalInterface
    private interface ValueSupplier {
        String get();
    }

    private record CachedValue<T>(T value, long expiresAt) {
    }

    private record ParsedPlaceholder(String dataKey, String suffix) {
    }
}
