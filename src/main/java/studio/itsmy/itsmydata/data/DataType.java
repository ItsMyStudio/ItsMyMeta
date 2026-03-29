package studio.itsmy.itsmydata.data;

import java.util.Locale;

public enum DataType {
    STRING,
    NUMBER;

    public static DataType fromConfigValue(String value) {
        return DataType.valueOf(value.trim().toUpperCase(Locale.ROOT));
    }
}
