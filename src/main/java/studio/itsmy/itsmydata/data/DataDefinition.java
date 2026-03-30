package studio.itsmy.itsmydata.data;

import java.util.regex.Pattern;
import studio.itsmy.itsmydata.leaderboard.LeaderboardSettings;
import studio.itsmy.itsmydata.scope.ScopeType;

public record DataDefinition(
    String key,
    DataType dataType,
    boolean decimal,
    ScopeType scopeType,
    Object defaultValue,
    Double minValue,
    Double maxValue,
    LeaderboardSettings leaderboardSettings
) {

    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("%[A-Za-z0-9_:.\\-]+%");
    private static final NumericExpressionResolver NUMERIC_EXPRESSION_RESOLVER = new NumericExpressionResolver();

    public DataDefinition {
        defaultValue = normalizeDefaultValue(dataType, decimal, defaultValue);
        validateBoundsConfiguration(key, dataType, minValue, maxValue);
        validateDefaultValue(key, dataType, decimal, defaultValue);
    }

    private static Object normalizeDefaultValue(DataType dataType, boolean decimal, Object defaultValue) {
        if (defaultValue != null) {
            return dataType == DataType.STRING ? String.valueOf(defaultValue) : defaultValue;
        }
        if (dataType == DataType.STRING) {
            return "";
        }
        if (decimal) {
            return 0D;
        }
        return 0;
    }

    private static void validateBoundsConfiguration(String key, DataType dataType, Double minValue, Double maxValue) {
        boolean numeric = dataType == DataType.NUMBER;
        if (!numeric && (minValue != null || maxValue != null)) {
            throw new IllegalStateException("Data '" + key + "' cannot define min/max when it is not numeric.");
        }
        if (minValue != null && maxValue != null && minValue > maxValue) {
            throw new IllegalStateException("Data '" + key + "' has min greater than max.");
        }
    }

    private static void validateDefaultValue(String key, DataType dataType, boolean decimal, Object defaultValue) {
        if (dataType == DataType.STRING) {
            return;
        }
        if (containsPlaceholderMarkers(defaultValue)) {
            return;
        }

        try {
            if (defaultValue instanceof Number number) {
                requireNumberType(key, decimal, number.doubleValue());
                return;
            }
            NUMERIC_EXPRESSION_RESOLVER.resolve(String.valueOf(defaultValue).trim(), decimal);
        } catch (IllegalArgumentException | IllegalStateException exception) {
            throw new IllegalStateException("Data '" + key + "' has an invalid default value.", exception);
        }
    }

    private static boolean containsPlaceholderMarkers(Object value) {
        return value instanceof String stringValue && PLACEHOLDER_PATTERN.matcher(stringValue).find();
    }

    private static void requireNumberType(String key, boolean decimal, double value) {
        if (!decimal && Math.rint(value) != value) {
            throw new IllegalArgumentException("Data '" + key + "' requires a whole number.");
        }
    }
}
