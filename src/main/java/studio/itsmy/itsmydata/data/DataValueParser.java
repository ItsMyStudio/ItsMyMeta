package studio.itsmy.itsmydata.data;

public final class DataValueParser {
    public Object parse(String rawValue, DataDefinition definition) {
        return definition.dataType() == DataType.NUMBER
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
