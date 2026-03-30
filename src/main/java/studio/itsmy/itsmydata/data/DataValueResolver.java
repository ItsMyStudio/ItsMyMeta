package studio.itsmy.itsmydata.data;

import java.util.Optional;
import org.bukkit.OfflinePlayer;
import studio.itsmy.itsmydata.papi.PlaceholderValueResolver;

final class DataValueResolver {

    private final PlaceholderValueResolver placeholderValueResolver;
    private final NumericExpressionResolver numericExpressionResolver;

    DataValueResolver() {
        this.placeholderValueResolver = new PlaceholderValueResolver();
        this.numericExpressionResolver = new NumericExpressionResolver();
    }

    Object resolveDefaultValue(OfflinePlayer player, DataDefinition definition) {
        return resolveEffectiveValue(player, definition, definition.defaultValue());
    }

    Object resolveStoredValue(OfflinePlayer player, DataDefinition definition, Optional<String> storedValue) {
        return storedValue
            .map(value -> resolveEffectiveValue(player, definition, value))
            .orElseGet(() -> resolveDefaultValue(player, definition));
    }

    Object resolveEffectiveValue(OfflinePlayer player, DataDefinition definition, Object rawValue) {
        return switch (definition.dataType()) {
            case STRING -> resolveStringValue(player, rawValue);
            case NUMBER -> resolveNumericValue(player, rawValue, definition);
        };
    }

    boolean containsPlaceholderMarkers(Object value) {
        return value instanceof String stringValue && placeholderValueResolver.containsPlaceholderMarkers(stringValue);
    }

    double toDouble(Object value) {
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        throw new IllegalStateException("Value is not numeric.");
    }

    Object castNumber(double value, DataDefinition definition) {
        if (definition.minValue() != null) {
            value = Math.max(value, definition.minValue());
        }
        if (definition.maxValue() != null) {
            value = Math.min(value, definition.maxValue());
        }
        if (definition.decimal()) {
            return value;
        }
        if (Math.rint(value) != value) {
            throw new IllegalArgumentException("Data '" + definition.key() + "' requires a whole number.");
        }
        return (int) value;
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
        String resolved = containsPlaceholderMarkers(rawString)
            ? placeholderValueResolver.resolve(player, rawString)
            : rawString;
        Object resolvedValue = numericExpressionResolver.resolve(resolved, definition.decimal());
        return castNumber(toDouble(resolvedValue), definition);
    }
}
