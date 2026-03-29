package studio.itsmy.itsmydata.db;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

public final class DatabaseSettingsLoader {

    public DatabaseSettings load(FileConfiguration configuration) {
        ConfigurationSection databaseSection = configuration.getConfigurationSection("database");
        if (databaseSection == null) {
            throw new IllegalStateException("Missing 'database' section in config.yml");
        }

        DatabaseType type = DatabaseType.fromConfigValue(databaseSection.getString("type", "sqlite"));

        ConfigurationSection sqliteSection = databaseSection.getConfigurationSection("sqlite");
        ConfigurationSection mysqlSection = databaseSection.getConfigurationSection("mysql");

        String sqliteFile = sqliteSection != null ? sqliteSection.getString("file", "data.db") : "data.db";
        String host = mysqlSection != null ? mysqlSection.getString("host", "localhost") : "localhost";
        int port = mysqlSection != null ? mysqlSection.getInt("port", 3306) : 3306;
        String database = mysqlSection != null ? mysqlSection.getString("database", "itsmydata") : "itsmydata";
        String username = mysqlSection != null ? mysqlSection.getString("username", "root") : "root";
        String password = mysqlSection != null ? mysqlSection.getString("password", "") : "";

        return new DatabaseSettings(type, sqliteFile, host, port, database, username, password);
    }
}
