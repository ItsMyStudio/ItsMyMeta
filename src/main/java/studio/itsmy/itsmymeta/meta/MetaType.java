package studio.itsmy.itsmymeta.meta;

import java.util.Locale;

public enum MetaType {
    STRING,
    NUMBER;

    public static MetaType fromConfigValue(String value) {
        return MetaType.valueOf(value.trim().toUpperCase(Locale.ROOT));
    }
}
