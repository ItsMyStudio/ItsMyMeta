package studio.itsmy.itsmymeta.papi;

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
import studio.itsmy.itsmymeta.ItsMyMetaPlugin;
import studio.itsmy.itsmymeta.leaderboard.LeaderboardPlacement;
import studio.itsmy.itsmymeta.meta.MetaDefinition;
import studio.itsmy.itsmymeta.meta.MetaType;
import studio.itsmy.itsmymeta.meta.MetaService;

public final class ItsMyMetaExpansion extends PlaceholderExpansion {

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

    private final ItsMyMetaPlugin plugin;
    private final MetaService metaService;
    private final Map<String, CachedValue<String>> leaderboardValueCache;
    private final Map<String, CachedValue<String>> leaderboardPositionCache;
    private final Map<String, CachedValue<String>> leaderboardTotalPlayersCache;
    private final Set<String> loggedErrors;

    public ItsMyMetaExpansion(ItsMyMetaPlugin plugin, MetaService metaService) {
        this.plugin = plugin;
        this.metaService = metaService;
        this.leaderboardValueCache = new ConcurrentHashMap<>();
        this.leaderboardPositionCache = new ConcurrentHashMap<>();
        this.leaderboardTotalPlayersCache = new ConcurrentHashMap<>();
        this.loggedErrors = ConcurrentHashMap.newKeySet();
    }

    @Override
    public @NotNull String getIdentifier() {
        return "itsmymeta";
    }

    @Override
    public @NotNull String getAuthor() {
        return "ItsMyDesktop";
    }

    @Override
    public @NotNull String getVersion() {
        return plugin.getPluginMeta().getVersion();
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

            MetaDefinition definition = resolveDefinition(params);
            ParsedPlaceholder placeholder = parsePlaceholder(params, definition.key());

            return switch (placeholder.suffix()) {
                case "_default" -> String.valueOf(metaService.getDefaultValue(player, definition.key()));
                case "_min" -> formatBound(definition.minValue());
                case "_max" -> formatBound(definition.maxValue());
                case "_fixed" -> formatFixed(metaService.getValue(player, definition.key()), definition.metaType());
                case "_formatted" -> formatCompact(metaService.getValue(player, definition.key()), definition.metaType());
                case "_commas" -> formatWithCommas(metaService.getValue(player, definition.key()), definition.metaType());
                default -> String.valueOf(metaService.getValue(player, definition.key()));
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

        String metaKey = matcher.group(1);
        int position = Integer.parseInt(matcher.group(2));
        String format = matcher.group(3);

        return getCachedValue(leaderboardValueCache, params, () -> {
            MetaDefinition definition = metaService.getDefinition(metaKey);
            if (position > metaService.getLeaderboardTotalEntries(metaKey)) {
                return fallbackLeaderboardValue(player, definition, format);
            }
            if ("name".equals(format)) {
                return metaService.getLeaderboardName(metaKey, position);
            }
            Object value = metaService.getLeaderboardValue(metaKey, position);

            if ("fixed".equals(format)) {
                return formatFixed(value, definition.metaType());
            }
            if ("formatted".equals(format)) {
                return formatCompact(value, definition.metaType());
            }
            if ("commas".equals(format)) {
                return formatWithCommas(value, definition.metaType());
            }
            return String.valueOf(value);
        });
    }

    private String fallbackLeaderboardValue(OfflinePlayer player, MetaDefinition definition, String format) {
        if ("name".equals(format)) {
            return plugin.getMessageService().raw("messages.common.none");
        }

        Object defaultValue = player != null
            ? metaService.getDefaultValue(player, definition.key())
            : definition.defaultValue();

        if ("fixed".equals(format)) {
            return formatFixed(defaultValue, definition.metaType());
        }
        if ("formatted".equals(format)) {
            return formatCompact(defaultValue, definition.metaType());
        }
        if ("commas".equals(format)) {
            return formatWithCommas(defaultValue, definition.metaType());
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

        String metaKey = matcher.group(1);
        String mode = matcher.group(2);
        String cacheKey = metaKey + "|" + player.getUniqueId() + "|" + (mode == null ? "rank" : mode);
        return getCachedValue(leaderboardPositionCache, cacheKey, () -> {
            LeaderboardPlacement placement = metaService.getLeaderboardPlacement(player, metaKey);
            return "percent".equals(mode) ? placement.displayPercent() : placement.displayRank();
        });
    }

    private String resolveLeaderboardTotalPlayersPlaceholder(String params) {
        if (!params.endsWith("_leaderboard_total_players")) {
            return null;
        }

        String metaKey = params.substring(0, params.length() - "_leaderboard_total_players".length());
        return getCachedValue(
            leaderboardTotalPlayersCache,
            metaKey,
            () -> String.valueOf(metaService.getLeaderboardTotalEntries(metaKey))
        );
    }

    private MetaDefinition resolveDefinition(String params) {
        try {
            return metaService.getDefinition(params);
        } catch (IllegalArgumentException ignored) {
            ParsedPlaceholder placeholder = parsePlaceholder(params, null);
            return metaService.getDefinition(placeholder.metaKey());
        }
    }

    private ParsedPlaceholder parsePlaceholder(String params, String exactMetaKey) {
        if (exactMetaKey != null && exactMetaKey.equalsIgnoreCase(params)) {
            return new ParsedPlaceholder(params, "");
        }

        for (String suffix : SUPPORTED_SUFFIXES) {
            if (params.endsWith(suffix) && params.length() > suffix.length()) {
                String metaKey = params.substring(0, params.length() - suffix.length());
                return new ParsedPlaceholder(metaKey, suffix);
            }
        }
        return new ParsedPlaceholder(params, "");
    }

    private String formatBound(Double bound) {
        return bound == null ? "" : stripTrailingZeros(bound);
    }

    private String formatFixed(Object value, MetaType metaType) {
        if (!isNumeric(metaType)) {
            return String.valueOf(value);
        }

        return FIXED_FORMAT.get().format(asDouble(value));
    }

    private String formatWithCommas(Object value, MetaType metaType) {
        if (!isNumeric(metaType)) {
            return String.valueOf(value);
        }

        return COMMA_FORMAT.get().format(asDouble(value));
    }

    private String formatCompact(Object value, MetaType metaType) {
        if (!isNumeric(metaType)) {
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

    private boolean isNumeric(MetaType metaType) {
        return metaType == MetaType.NUMBER;
    }

    private double asDouble(Object value) {
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        throw new IllegalArgumentException("Meta value is not numeric.");
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

    private record ParsedPlaceholder(String metaKey, String suffix) {
    }
}
