package studio.itsmy.itsmymeta.db;

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
