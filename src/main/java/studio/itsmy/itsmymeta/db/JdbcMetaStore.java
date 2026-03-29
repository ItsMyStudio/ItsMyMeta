package studio.itsmy.itsmymeta.db;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.io.File;
import java.sql.DatabaseMetaData;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import org.bukkit.plugin.java.JavaPlugin;
import studio.itsmy.itsmymeta.leaderboard.LeaderboardEntry;
import studio.itsmy.itsmymeta.meta.MetaStore;
import studio.itsmy.itsmymeta.scope.ScopeContext;
import studio.itsmy.itsmymeta.scope.ScopeType;

public final class JdbcMetaStore implements MetaStore {

    private final JavaPlugin plugin;
    private final DatabaseSettings settings;
    private HikariDataSource dataSource;

    public JdbcMetaStore(JavaPlugin plugin, DatabaseSettings settings) {
        this.plugin = plugin;
        this.settings = settings;
    }

    @Override
    public void initialize() {
        HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setPoolName("ItsMyMetaPool");

        switch (settings.type()) {
            case SQLITE -> configureSqlite(hikariConfig);
            case MYSQL -> configureMysql(hikariConfig);
        }

        this.dataSource = new HikariDataSource(hikariConfig);
        createTables();
    }

    @Override
    public Optional<String> findValue(ScopeContext scope, String metaKey) {
        String sql = """
            SELECT meta_value
            FROM meta_values
            WHERE meta_key = ? AND scope_type = ? AND scope_id = ?
            """;

        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, metaKey);
            statement.setString(2, scope.type().name());
            statement.setString(3, scope.id());

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return Optional.ofNullable(resultSet.getString("meta_value"));
                }
                return Optional.empty();
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Could not read meta value from database.", exception);
        }
    }

    @Override
    public void saveValueAndSyncLeaderboard(ScopeContext scope, String metaKey, String value, Double leaderboardValue) {
        try (Connection connection = dataSource.getConnection()) {
            connection.setAutoCommit(false);
            try {
                Optional<String> currentValue = findValue(connection, scope, metaKey);
                Optional<Double> currentLeaderboardValue = findLeaderboardValue(connection, scope, metaKey);

                if (currentValue.filter(value::equals).isEmpty()) {
                    try (PreparedStatement metaStatement = connection.prepareStatement(metaValueUpsertSql())) {
                        bindMetaValueUpsert(metaStatement, scope, metaKey, value);
                        metaStatement.executeUpdate();
                    }
                }

                syncLeaderboard(connection, scope, metaKey, leaderboardValue, currentLeaderboardValue);
                connection.commit();
            } catch (SQLException exception) {
                connection.rollback();
                throw exception;
            } finally {
                connection.setAutoCommit(true);
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Could not save meta value and sync leaderboard cache.", exception);
        }
    }

    @Override
    public void deleteValueAndSyncLeaderboard(ScopeContext scope, String metaKey, Double leaderboardValue) {
        try (Connection connection = dataSource.getConnection()) {
            connection.setAutoCommit(false);
            try {
                Optional<String> currentValue = findValue(connection, scope, metaKey);
                Optional<Double> currentLeaderboardValue = findLeaderboardValue(connection, scope, metaKey);

                if (currentValue.isPresent()) {
                    try (PreparedStatement metaStatement = connection.prepareStatement(deleteMetaValueSql())) {
                        bindScopeKey(metaStatement, scope, metaKey);
                        metaStatement.executeUpdate();
                    }
                }

                syncLeaderboard(connection, scope, metaKey, leaderboardValue, currentLeaderboardValue);
                connection.commit();
            } catch (SQLException exception) {
                connection.rollback();
                throw exception;
            } finally {
                connection.setAutoCommit(true);
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Could not delete meta value and sync leaderboard cache.", exception);
        }
    }

    @Override
    public void saveLeaderboardValue(ScopeContext scope, String metaKey, double numericValue) {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(leaderboardUpsertSql())) {
            bindLeaderboardUpsert(statement, scope, metaKey, numericValue);
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw new IllegalStateException("Could not save leaderboard cache value.", exception);
        }
    }

    @Override
    public void deleteScope(ScopeContext scope) {
        try (Connection connection = dataSource.getConnection()) {
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
            throw new IllegalStateException("Could not delete meta scope from database.", exception);
        }
    }

    @Override
    public List<LeaderboardEntry> getTopLeaderboard(String metaKey, int limit) {
        String sql = """
            SELECT meta_key, scope_type, scope_id, numeric_value
            FROM meta_leaderboard_cache
            WHERE meta_key = ?
            ORDER BY numeric_value DESC, scope_id ASC
            LIMIT ?
            """;

        List<LeaderboardEntry> entries = new ArrayList<>();
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, metaKey);
            statement.setInt(2, limit);

            try (ResultSet resultSet = statement.executeQuery()) {
                int rank = 1;
                while (resultSet.next()) {
                    entries.add(new LeaderboardEntry(
                        rank++,
                        resultSet.getString("meta_key"),
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
    public Optional<Integer> findRankInTop(String metaKey, ScopeContext scope, int limit) {
        String sql = """
            SELECT scope_type, scope_id
            FROM meta_leaderboard_cache
            WHERE meta_key = ?
            ORDER BY numeric_value DESC, scope_id ASC
            LIMIT ?
            """;

        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, metaKey);
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
    public int countLeaderboardEntries(String metaKey) {
        String sql = """
            SELECT COUNT(*)
            FROM meta_leaderboard_cache
            WHERE meta_key = ?
            """;

        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, metaKey);

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
    public List<ScopeContext> getLeaderboardScopes(String metaKey) {
        String sql = """
            SELECT scope_type, scope_id
            FROM meta_leaderboard_cache
            WHERE meta_key = ?
            """;

        List<ScopeContext> scopes = new ArrayList<>();
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, metaKey);

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
    public List<ScopeContext> getDynamicValueScopes(String metaKey) {
        String sql = switch (settings.type()) {
            case SQLITE -> """
                SELECT scope_type, scope_id
                FROM meta_values
                WHERE meta_key = ?
                  AND instr(meta_value, '%') > 0
                  AND instr(substr(meta_value, instr(meta_value, '%') + 1), '%') > 1
                """;
            case MYSQL -> """
                SELECT scope_type, scope_id
                FROM meta_values
                WHERE meta_key = ?
                  AND LOCATE('%', meta_value) > 0
                  AND LOCATE('%', meta_value, LOCATE('%', meta_value) + 1) > LOCATE('%', meta_value) + 1
                """;
        };

        List<ScopeContext> scopes = new ArrayList<>();
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, metaKey);

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
    public void close() {
        if (dataSource != null) {
            dataSource.close();
        }
    }

    private void configureSqlite(HikariConfig hikariConfig) {
        if (!plugin.getDataFolder().exists()) {
            plugin.getDataFolder().mkdirs();
        }

        File sqliteFile = new File(plugin.getDataFolder(), settings.sqliteFile());
        hikariConfig.setJdbcUrl("jdbc:sqlite:" + sqliteFile.getAbsolutePath());
        hikariConfig.setMaximumPoolSize(1);
        hikariConfig.addDataSourceProperty("foreign_keys", "true");
    }

    private void configureMysql(HikariConfig hikariConfig) {
        String jdbcUrl = "jdbc:mysql://" + settings.host() + ":" + settings.port() + "/" + settings.database()
            + "?useSSL=false&allowPublicKeyRetrieval=true&characterEncoding=utf8";

        hikariConfig.setJdbcUrl(jdbcUrl);
        hikariConfig.setUsername(settings.username());
        hikariConfig.setPassword(settings.password());
        hikariConfig.setMaximumPoolSize(10);
    }

    private void createTables() {
        String metaValuesSql = """
            CREATE TABLE IF NOT EXISTS meta_values (
                meta_key VARCHAR(100) NOT NULL,
                scope_type VARCHAR(50) NOT NULL,
                scope_id VARCHAR(150) NOT NULL,
                meta_value TEXT NOT NULL,
                updated_at BIGINT NOT NULL,
                PRIMARY KEY (meta_key, scope_type, scope_id)
            )
            """;
        String leaderboardSql = """
            CREATE TABLE IF NOT EXISTS meta_leaderboard_cache (
                meta_key VARCHAR(100) NOT NULL,
                scope_type VARCHAR(50) NOT NULL,
                scope_id VARCHAR(150) NOT NULL,
                numeric_value DOUBLE NOT NULL,
                updated_at BIGINT NOT NULL,
                PRIMARY KEY (meta_key, scope_type, scope_id)
            )
            """;
        String leaderboardIndexSql = switch (settings.type()) {
            case SQLITE -> """
                CREATE INDEX IF NOT EXISTS idx_meta_leaderboard_value
                ON meta_leaderboard_cache (meta_key, numeric_value DESC, scope_id ASC)
                """;
            case MYSQL -> """
                CREATE INDEX idx_meta_leaderboard_value
                ON meta_leaderboard_cache (meta_key, numeric_value DESC, scope_id ASC)
                """;
        };

        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {
            statement.execute(metaValuesSql);
            statement.execute(leaderboardSql);
            if (!indexExists(connection, "meta_leaderboard_cache", "idx_meta_leaderboard_value")) {
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
        String metaKey,
        Double leaderboardValue,
        Optional<Double> currentLeaderboardValue
    ) throws SQLException {
        if (leaderboardValue == null) {
            if (currentLeaderboardValue.isEmpty()) {
                return;
            }
            try (PreparedStatement leaderboardStatement = connection.prepareStatement(deleteLeaderboardSql())) {
                bindScopeKey(leaderboardStatement, scope, metaKey);
                leaderboardStatement.executeUpdate();
            }
            return;
        }

        if (currentLeaderboardValue.isPresent() && Double.compare(currentLeaderboardValue.get(), leaderboardValue) == 0) {
            return;
        }

        try (PreparedStatement leaderboardStatement = connection.prepareStatement(leaderboardUpsertSql())) {
            bindLeaderboardUpsert(leaderboardStatement, scope, metaKey, leaderboardValue);
            leaderboardStatement.executeUpdate();
        }
    }

    private String metaValueUpsertSql() {
        return switch (settings.type()) {
            case SQLITE -> """
                INSERT INTO meta_values (meta_key, scope_type, scope_id, meta_value, updated_at)
                VALUES (?, ?, ?, ?, ?)
                ON CONFLICT(meta_key, scope_type, scope_id)
                DO UPDATE SET meta_value = excluded.meta_value, updated_at = excluded.updated_at
                """;
            case MYSQL -> """
                INSERT INTO meta_values (meta_key, scope_type, scope_id, meta_value, updated_at)
                VALUES (?, ?, ?, ?, ?)
                ON DUPLICATE KEY UPDATE meta_value = VALUES(meta_value), updated_at = VALUES(updated_at)
                """;
        };
    }

    private String leaderboardUpsertSql() {
        return switch (settings.type()) {
            case SQLITE -> """
                INSERT INTO meta_leaderboard_cache (meta_key, scope_type, scope_id, numeric_value, updated_at)
                VALUES (?, ?, ?, ?, ?)
                ON CONFLICT(meta_key, scope_type, scope_id)
                DO UPDATE SET numeric_value = excluded.numeric_value, updated_at = excluded.updated_at
                """;
            case MYSQL -> """
                INSERT INTO meta_leaderboard_cache (meta_key, scope_type, scope_id, numeric_value, updated_at)
                VALUES (?, ?, ?, ?, ?)
                ON DUPLICATE KEY UPDATE numeric_value = VALUES(numeric_value), updated_at = VALUES(updated_at)
                """;
        };
    }

    private String deleteMetaValueSql() {
        return """
            DELETE FROM meta_values
            WHERE meta_key = ? AND scope_type = ? AND scope_id = ?
            """;
    }

    private String deleteLeaderboardSql() {
        return """
            DELETE FROM meta_leaderboard_cache
            WHERE meta_key = ? AND scope_type = ? AND scope_id = ?
            """;
    }

    private String deleteScopeValuesSql() {
        return """
            DELETE FROM meta_values
            WHERE scope_type = ? AND scope_id = ?
            """;
    }

    private String deleteScopeLeaderboardSql() {
        return """
            DELETE FROM meta_leaderboard_cache
            WHERE scope_type = ? AND scope_id = ?
            """;
    }

    private void bindScopeKey(PreparedStatement statement, ScopeContext scope, String metaKey) throws SQLException {
        statement.setString(1, metaKey);
        statement.setString(2, scope.type().name());
        statement.setString(3, scope.id());
    }

    private void bindMetaValueUpsert(PreparedStatement statement, ScopeContext scope, String metaKey, String value) throws SQLException {
        bindScopeKey(statement, scope, metaKey);
        statement.setString(4, value);
        statement.setLong(5, System.currentTimeMillis());
    }

    private void bindScopeOnly(PreparedStatement statement, ScopeContext scope) throws SQLException {
        statement.setString(1, scope.type().name());
        statement.setString(2, scope.id());
    }

    private void bindLeaderboardUpsert(PreparedStatement statement, ScopeContext scope, String metaKey, double numericValue) throws SQLException {
        bindScopeKey(statement, scope, metaKey);
        statement.setDouble(4, numericValue);
        statement.setLong(5, System.currentTimeMillis());
    }

    private boolean indexExists(Connection connection, String tableName, String indexName) throws SQLException {
        DatabaseMetaData metaData = connection.getMetaData();
        try (ResultSet resultSet = metaData.getIndexInfo(null, null, tableName, false, false)) {
            while (resultSet.next()) {
                String existingIndexName = resultSet.getString("INDEX_NAME");
                if (existingIndexName != null && existingIndexName.equalsIgnoreCase(indexName)) {
                    return true;
                }
            }
        }
        return false;
    }

    private Optional<String> findValue(Connection connection, ScopeContext scope, String metaKey) throws SQLException {
        String sql = """
            SELECT meta_value
            FROM meta_values
            WHERE meta_key = ? AND scope_type = ? AND scope_id = ?
            """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            bindScopeKey(statement, scope, metaKey);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return Optional.ofNullable(resultSet.getString("meta_value"));
                }
                return Optional.empty();
            }
        }
    }

    private Optional<Double> findLeaderboardValue(Connection connection, ScopeContext scope, String metaKey) throws SQLException {
        String sql = """
            SELECT numeric_value
            FROM meta_leaderboard_cache
            WHERE meta_key = ? AND scope_type = ? AND scope_id = ?
            """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            bindScopeKey(statement, scope, metaKey);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return Optional.of(resultSet.getDouble("numeric_value"));
                }
                return Optional.empty();
            }
        }
    }
}
