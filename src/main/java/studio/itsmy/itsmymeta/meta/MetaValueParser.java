package studio.itsmy.itsmymeta.meta;

public final class MetaValueParser {
    public Object parse(String rawValue, MetaDefinition definition) {
        return definition.metaType() == MetaType.NUMBER
            ? parseNumber(rawValue, definition.decimal())
            : rawValue;
    }

    public Object parseNumber(String rawValue, boolean decimal) {
        return decimal ? Double.parseDouble(rawValue) : Integer.parseInt(rawValue);
    }

    public String serialize(Object value) {
        return String.valueOf(value);
    }
}
