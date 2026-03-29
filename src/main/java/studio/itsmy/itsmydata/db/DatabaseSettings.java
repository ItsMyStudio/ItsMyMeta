package studio.itsmy.itsmydata.db;

public record DatabaseSettings(
    DatabaseType type,
    String sqliteFile,
    String host,
    int port,
    String database,
    String username,
    String password
) {
}
