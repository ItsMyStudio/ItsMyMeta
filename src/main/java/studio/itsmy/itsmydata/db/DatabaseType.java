package studio.itsmy.itsmydata.db;

import java.util.Locale;

public enum DatabaseType {
    SQLITE,
    MYSQL;

    public static DatabaseType fromConfigValue(String value) {
        return DatabaseType.valueOf(value.trim().toUpperCase(Locale.ROOT));
    }
}
