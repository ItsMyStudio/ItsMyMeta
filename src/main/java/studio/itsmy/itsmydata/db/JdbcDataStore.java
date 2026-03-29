package studio.itsmy.itsmydata.db;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.io.File;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import org.bukkit.plugin.java.JavaPlugin;
import studio.itsmy.itsmydata.leaderboard.LeaderboardEntry;
import studio.itsmy.itsmydata.data.DataStore;
import studio.itsmy.itsmydata.scope.ScopeContext;
import studio.itsmy.itsmydata.scope.ScopeType;

public final class JdbcDataStore implements DataStore {

    private final JavaPlugin plugin;
    private volatile DatabaseSettings settings;
    private volatile HikariDataSource dataSource;

    public JdbcDataStore(JavaPlugin plugin, DatabaseSettings settings) {
        this.plugin = plugin;
        this.settings = settings;
    }

    @Override
    public synchronized void initialize() {
        replaceDataSource(settings);
    }

    public synchronized void reload(DatabaseSettings settings) {
        this.settings = settings;
        replaceDataSource(settings);
    }

    private void replaceDataSource(DatabaseSettings databaseSettings) {
        HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setPoolName("ItsMyDataPool");

        switch (databaseSettings.type()) {
            case SQLITE -> configureSqlite(hikariConfig, databaseSettings);
            case MYSQL -> configureMysql(hikariConfig, databaseSettings);
        }

        HikariDataSource newDataSource = new HikariDataSource(hikariConfig);
        try {
            createTables(newDataSource, databaseSettings);
        } catch (RuntimeException exception) {
            newDataSource.close();
            throw exception;
        }

        HikariDataSource previousDataSource = this.dataSource;
        this.dataSource = newDataSource;
        if (previousDataSource != null) {
            previousDataSource.close();
        }
    }

    @Override
    public Optional<String> findValue(ScopeContext scope, String dataKey) {
        String sql = """
            SELECT data_value
            FROM data_values
            WHERE data_key = ? AND scope_type = ? AND scope_id = ?
            """;

        try (Connection connection = requireDataSource().getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, dataKey);
            statement.setString(2, scope.type().name());
            statement.setString(3, scope.id());

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return Optional.ofNullable(resultSet.getString("data_value"));
                }
                return Optional.empty();
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Could not read data value from database.", exception);
        }
    }

    @Override
    public void saveValueAndSyncLeaderboard(ScopeContext scope, String dataKey, String value, Double leaderboardValue) {
        try (Connection connection = requireDataSource().getConnection()) {
            connection.setAutoCommit(false);
            try {
                Optional<String> currentValue = findValue(connection, scope, dataKey);
                Optional<Double> currentLeaderboardValue = findLeaderboardValue(connection, scope, dataKey);

                if (currentValue.filter(value::equals).isEmpty()) {
                    try (PreparedStatement dataStatement = connection.prepareStatement(dataValueUpsertSql())) {
                        bindDataValueUpsert(dataStatement, scope, dataKey, value);
                        dataStatement.executeUpdate();
                    }
                }

                syncLeaderboard(connection, scope, dataKey, leaderboardValue, currentLeaderboardValue);
                connection.commit();
            } catch (SQLException exception) {
                connection.rollback();
                throw exception;
            } finally {
                connection.setAutoCommit(true);
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Could not save data value and sync leaderboard cache.", exception);
        }
    }

    @Override
    public void deleteValueAndSyncLeaderboard(ScopeContext scope, String dataKey, Double leaderboardValue) {
        try (Connection connection = requireDataSource().getConnection()) {
            connection.setAutoCommit(false);
            try {
                Optional<String> currentValue = findValue(connection, scope, dataKey);
                Optional<Double> currentLeaderboardValue = findLeaderboardValue(connection, scope, dataKey);

                if (currentValue.isPresent()) {
                    try (PreparedStatement dataStatement = connection.prepareStatement(deleteDataValueSql())) {
                        bindScopeKey(dataStatement, scope, dataKey);
                        dataStatement.executeUpdate();
                    }
                }

                syncLeaderboard(connection, scope, dataKey, leaderboardValue, currentLeaderboardValue);
                connection.commit();
            } catch (SQLException exception) {
                connection.rollback();
                throw exception;
            } finally {
                connection.setAutoCommit(true);
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Could not delete data value and sync leaderboard cache.", exception);
        }
    }

    @Override
    public void saveLeaderboardValue(ScopeContext scope, String dataKey, double numericValue) {
        try (Connection connection = requireDataSource().getConnection();
             PreparedStatement statement = connection.prepareStatement(leaderboardUpsertSql())) {
            bindLeaderboardUpsert(statement, scope, dataKey, numericValue);
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw new IllegalStateException("Could not save leaderboard cache value.", exception);
        }
    }

    @Override
    public void deleteScope(ScopeContext scope) {
        try (Connection connection = requireDataSource().getConnection()) {
            connection.setAutoCommit(false);
            try {
                try (PreparedStatement valuesStatement = connection.prepareStatement(deleteScopeValuesSql());
                     PreparedStatement leaderboardStatement = connection.prepareStatement(deleteScopeLeaderboardSql())) {
                    bindScopeOnly(valuesStatement, scope);
                    valuesStatement.executeUpdate();

                    bindScopeOnly(leaderboardStatement, scope);
                    leaderboardStatement.executeUpdate();
                }
                connection.commit();
            } catch (SQLException exception) {
                connection.rollback();
                throw exception;
            } finally {
                connection.setAutoCommit(true);
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Could not delete data scope from database.", exception);
        }
    }

    @Override
    public List<LeaderboardEntry> getTopLeaderboard(String dataKey, int limit) {
        String sql = """
            SELECT data_key, scope_type, scope_id, numeric_value
            FROM data_leaderboard_cache
            WHERE data_key = ?
            ORDER BY numeric_value DESC, scope_id ASC
            LIMIT ?
            """;

        List<LeaderboardEntry> entries = new ArrayList<>();
        try (Connection connection = requireDataSource().getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, dataKey);
            statement.setInt(2, limit);

            try (ResultSet resultSet = statement.executeQuery()) {
                int rank = 1;
                while (resultSet.next()) {
                    entries.add(new LeaderboardEntry(
                        rank++,
                        resultSet.getString("data_key"),
                        ScopeType.valueOf(resultSet.getString("scope_type")),
                        resultSet.getString("scope_id"),
                        resultSet.getDouble("numeric_value")
                    ));
                }
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Could not load leaderboard entries.", exception);
        }
        return entries;
    }

    @Override
    public Optional<Integer> findRankInTop(String dataKey, ScopeContext scope, int limit) {
        String sql = """
            SELECT scope_type, scope_id
            FROM data_leaderboard_cache
            WHERE data_key = ?
            ORDER BY numeric_value DESC, scope_id ASC
            LIMIT ?
            """;

        try (Connection connection = requireDataSource().getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, dataKey);
            statement.setInt(2, limit);

            try (ResultSet resultSet = statement.executeQuery()) {
                int rank = 1;
                while (resultSet.next()) {
                    String scopeType = resultSet.getString("scope_type");
                    String scopeId = resultSet.getString("scope_id");
                    if (scope.type().name().equals(scopeType) && scope.id().equals(scopeId)) {
                        return Optional.of(rank);
                    }
                    rank++;
                }
            }
            return Optional.empty();
        } catch (SQLException exception) {
            throw new IllegalStateException("Could not determine leaderboard rank.", exception);
        }
    }

    @Override
    public int countLeaderboardEntries(String dataKey) {
        String sql = """
            SELECT COUNT(*)
            FROM data_leaderboard_cache
            WHERE data_key = ?
            """;

        try (Connection connection = requireDataSource().getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, dataKey);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return resultSet.getInt(1);
                }
                return 0;
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Could not count leaderboard entries.", exception);
        }
    }

    @Override
    public List<ScopeContext> getLeaderboardScopes(String dataKey) {
        String sql = """
            SELECT scope_type, scope_id
            FROM data_leaderboard_cache
            WHERE data_key = ?
            """;

        List<ScopeContext> scopes = new ArrayList<>();
        try (Connection connection = requireDataSource().getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, dataKey);

            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    scopes.add(new ScopeContext(
                        ScopeType.valueOf(resultSet.getString("scope_type")),
                        resultSet.getString("scope_id")
                    ));
                }
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Could not load leaderboard scopes.", exception);
        }
        return scopes;
    }

    @Override
    public List<ScopeContext> getDynamicValueScopes(String dataKey) {
        DatabaseSettings currentSettings = settings;
        String sql = switch (currentSettings.type()) {
            case SQLITE -> """
                SELECT scope_type, scope_id
                FROM data_values
                WHERE data_key = ?
                  AND instr(data_value, '%') > 0
                  AND instr(substr(data_value, instr(data_value, '%') + 1), '%') > 1
                """;
            case MYSQL -> """
                SELECT scope_type, scope_id
                FROM data_values
                WHERE data_key = ?
                  AND LOCATE('%', data_value) > 0
                  AND LOCATE('%', data_value, LOCATE('%', data_value) + 1) > LOCATE('%', data_value) + 1
                """;
        };

        List<ScopeContext> scopes = new ArrayList<>();
        try (Connection connection = requireDataSource().getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, dataKey);

            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    scopes.add(new ScopeContext(
                        ScopeType.valueOf(resultSet.getString("scope_type")),
                        resultSet.getString("scope_id")
                    ));
                }
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Could not load dynamic leaderboard scopes.", exception);
        }
        return scopes;
    }

    @Override
    public synchronized void close() {
        if (dataSource != null) {
            dataSource.close();
            dataSource = null;
        }
    }

    private void configureSqlite(HikariConfig hikariConfig, DatabaseSettings databaseSettings) {
        if (!plugin.getDataFolder().exists()) {
            plugin.getDataFolder().mkdirs();
        }

        File sqliteFile = new File(plugin.getDataFolder(), databaseSettings.sqliteFile());
        hikariConfig.setJdbcUrl("jdbc:sqlite:" + sqliteFile.getAbsolutePath());
        hikariConfig.setMaximumPoolSize(1);
        hikariConfig.addDataSourceProperty("foreign_keys", "true");
    }

    private void configureMysql(HikariConfig hikariConfig, DatabaseSettings databaseSettings) {
        String jdbcUrl = "jdbc:mysql://" + databaseSettings.host() + ":" + databaseSettings.port() + "/" + databaseSettings.database()
            + "?useSSL=false&allowPublicKeyRetrieval=true&characterEncoding=utf8";

        hikariConfig.setJdbcUrl(jdbcUrl);
        hikariConfig.setUsername(databaseSettings.username());
        hikariConfig.setPassword(databaseSettings.password());
        hikariConfig.setMaximumPoolSize(10);
    }

    private void createTables(HikariDataSource dataSource, DatabaseSettings databaseSettings) {
        String dataValuesSql = """
            CREATE TABLE IF NOT EXISTS data_values (
                data_key VARCHAR(100) NOT NULL,
                scope_type VARCHAR(50) NOT NULL,
                scope_id VARCHAR(150) NOT NULL,
                data_value TEXT NOT NULL,
                updated_at BIGINT NOT NULL,
                PRIMARY KEY (data_key, scope_type, scope_id)
            )
            """;
        String leaderboardSql = """
            CREATE TABLE IF NOT EXISTS data_leaderboard_cache (
                data_key VARCHAR(100) NOT NULL,
                scope_type VARCHAR(50) NOT NULL,
                scope_id VARCHAR(150) NOT NULL,
                numeric_value DOUBLE NOT NULL,
                updated_at BIGINT NOT NULL,
                PRIMARY KEY (data_key, scope_type, scope_id)
            )
            """;
        String leaderboardIndexSql = switch (databaseSettings.type()) {
            case SQLITE -> """
                CREATE INDEX IF NOT EXISTS idx_data_leaderboard_value
                ON data_leaderboard_cache (data_key, numeric_value DESC, scope_id ASC)
                """;
            case MYSQL -> """
                CREATE INDEX idx_data_leaderboard_value
                ON data_leaderboard_cache (data_key, numeric_value DESC, scope_id ASC)
                """;
        };

        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {
            statement.execute(dataValuesSql);
            statement.execute(leaderboardSql);
            if (!indexExists(connection, "data_leaderboard_cache", "idx_data_leaderboard_value")) {
                statement.execute(leaderboardIndexSql);
            }
        } catch (SQLException exception) {
            plugin.getLogger().log(Level.SEVERE, "Could not initialize database schema.", exception);
            throw new IllegalStateException("Could not initialize database schema.", exception);
        }
    }

    private void syncLeaderboard(
        Connection connection,
        ScopeContext scope,
        String dataKey,
        Double leaderboardValue,
        Optional<Double> currentLeaderboardValue
    ) throws SQLException {
        if (leaderboardValue == null) {
            if (currentLeaderboardValue.isEmpty()) {
                return;
            }
            try (PreparedStatement leaderboardStatement = connection.prepareStatement(deleteLeaderboardSql())) {
                bindScopeKey(leaderboardStatement, scope, dataKey);
                leaderboardStatement.executeUpdate();
            }
            return;
        }

        if (currentLeaderboardValue.isPresent() && Double.compare(currentLeaderboardValue.get(), leaderboardValue) == 0) {
            return;
        }

        try (PreparedStatement leaderboardStatement = connection.prepareStatement(leaderboardUpsertSql())) {
            bindLeaderboardUpsert(leaderboardStatement, scope, dataKey, leaderboardValue);
            leaderboardStatement.executeUpdate();
        }
    }

    private String dataValueUpsertSql() {
        return switch (settings.type()) {
            case SQLITE -> """
                INSERT INTO data_values (data_key, scope_type, scope_id, data_value, updated_at)
                VALUES (?, ?, ?, ?, ?)
                ON CONFLICT(data_key, scope_type, scope_id)
                DO UPDATE SET data_value = excluded.data_value, updated_at = excluded.updated_at
                """;
            case MYSQL -> """
                INSERT INTO data_values (data_key, scope_type, scope_id, data_value, updated_at)
                VALUES (?, ?, ?, ?, ?)
                ON DUPLICATE KEY UPDATE data_value = VALUES(data_value), updated_at = VALUES(updated_at)
                """;
        };
    }

    private String leaderboardUpsertSql() {
        return switch (settings.type()) {
            case SQLITE -> """
                INSERT INTO data_leaderboard_cache (data_key, scope_type, scope_id, numeric_value, updated_at)
                VALUES (?, ?, ?, ?, ?)
                ON CONFLICT(data_key, scope_type, scope_id)
                DO UPDATE SET numeric_value = excluded.numeric_value, updated_at = excluded.updated_at
                """;
            case MYSQL -> """
                INSERT INTO data_leaderboard_cache (data_key, scope_type, scope_id, numeric_value, updated_at)
                VALUES (?, ?, ?, ?, ?)
                ON DUPLICATE KEY UPDATE numeric_value = VALUES(numeric_value), updated_at = VALUES(updated_at)
                """;
        };
    }

    private String deleteDataValueSql() {
        return """
            DELETE FROM data_values
            WHERE data_key = ? AND scope_type = ? AND scope_id = ?
            """;
    }

    private String deleteLeaderboardSql() {
        return """
            DELETE FROM data_leaderboard_cache
            WHERE data_key = ? AND scope_type = ? AND scope_id = ?
            """;
    }

    private String deleteScopeValuesSql() {
        return """
            DELETE FROM data_values
            WHERE scope_type = ? AND scope_id = ?
            """;
    }

    private String deleteScopeLeaderboardSql() {
        return """
            DELETE FROM data_leaderboard_cache
            WHERE scope_type = ? AND scope_id = ?
            """;
    }

    private void bindScopeKey(PreparedStatement statement, ScopeContext scope, String dataKey) throws SQLException {
        statement.setString(1, dataKey);
        statement.setString(2, scope.type().name());
        statement.setString(3, scope.id());
    }

    private void bindDataValueUpsert(PreparedStatement statement, ScopeContext scope, String dataKey, String value) throws SQLException {
        bindScopeKey(statement, scope, dataKey);
        statement.setString(4, value);
        statement.setLong(5, System.currentTimeMillis());
    }

    private void bindScopeOnly(PreparedStatement statement, ScopeContext scope) throws SQLException {
        statement.setString(1, scope.type().name());
        statement.setString(2, scope.id());
    }

    private void bindLeaderboardUpsert(PreparedStatement statement, ScopeContext scope, String dataKey, double numericValue) throws SQLException {
        bindScopeKey(statement, scope, dataKey);
        statement.setDouble(4, numericValue);
        statement.setLong(5, System.currentTimeMillis());
    }

    private boolean indexExists(Connection connection, String tableName, String indexName) throws SQLException {
        DatabaseMetaData metadata = connection.getMetaData();
        try (ResultSet resultSet = metadata.getIndexInfo(null, null, tableName, false, false)) {
            while (resultSet.next()) {
                String existingIndexName = resultSet.getString("INDEX_NAME");
                if (existingIndexName != null && existingIndexName.equalsIgnoreCase(indexName)) {
                    return true;
                }
            }
        }
        return false;
    }

    private HikariDataSource requireDataSource() {
        HikariDataSource currentDataSource = dataSource;
        if (currentDataSource == null) {
            throw new IllegalStateException("Data store has not been initialized.");
        }
        return currentDataSource;
    }

    private Optional<String> findValue(Connection connection, ScopeContext scope, String dataKey) throws SQLException {
        String sql = """
            SELECT data_value
            FROM data_values
            WHERE data_key = ? AND scope_type = ? AND scope_id = ?
            """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            bindScopeKey(statement, scope, dataKey);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return Optional.ofNullable(resultSet.getString("data_value"));
                }
                return Optional.empty();
            }
        }
    }

    private Optional<Double> findLeaderboardValue(Connection connection, ScopeContext scope, String dataKey) throws SQLException {
        String sql = """
            SELECT numeric_value
            FROM data_leaderboard_cache
            WHERE data_key = ? AND scope_type = ? AND scope_id = ?
            """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            bindScopeKey(statement, scope, dataKey);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return Optional.of(resultSet.getDouble("numeric_value"));
                }
                return Optional.empty();
            }
        }
    }
}
