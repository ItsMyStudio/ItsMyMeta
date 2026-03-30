package studio.itsmy.itsmydata.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.lang.reflect.Proxy;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;
import studio.itsmy.itsmydata.leaderboard.LeaderboardEntry;
import studio.itsmy.itsmydata.leaderboard.LeaderboardService;
import studio.itsmy.itsmydata.scope.ScopeContext;
import studio.itsmy.itsmydata.scope.ScopeResolver;

class DataServiceTest {

    @Test
    void getValueReturnsResolvedDefaultWhenStoreIsEmpty() {
        DataService service = newService(
            """
            data-definitions:
              coins:
                type: number
                scope: player
                default: 7
            """
        );

        assertEquals(7, service.getValue(testPlayer("Steve"), "coins"));
    }

    @Test
    void getValueReturnsResolvedStoredValueNotOptionalWrapper() {
        InMemoryDataStore dataStore = new InMemoryDataStore();
        DataService service = newService(
            """
            data-definitions:
              coins:
                type: number
                scope: player
                default: 0
                min: 0
                max: 100
            """,
            dataStore
        );

        OfflinePlayer player = testPlayer("Steve");
        dataStore.put(new ScopeContext(studio.itsmy.itsmydata.scope.ScopeType.PLAYER, player.getUniqueId().toString()), "coins", "150");

        assertEquals(100, service.getValue(player, "coins"));
    }

    @Test
    void validateDefinitionsRejectsDynamicLeaderboardPlaceholdersOutsidePlayerScope() {
        DataService service = newService(
            """
            data-definitions:
              global-balance:
                type: number
                scope: global
                default: "%player_level%"
                leaderboard:
                  enabled: true
            """
        );

        IllegalStateException exception = assertThrows(IllegalStateException.class, service::validateDefinitions);
        assertEquals(
            "Numeric leaderboard data 'global-balance' cannot use PlaceholderAPI values outside player scope.",
            exception.getMessage()
        );
    }

    private static DataService newService(String configText) {
        return newService(configText, new InMemoryDataStore());
    }

    private static DataService newService(String configText, InMemoryDataStore dataStore) {
        YamlConfiguration configuration = new YamlConfiguration();
        try {
            configuration.loadFromString(configText);
        } catch (InvalidConfigurationException exception) {
            throw new IllegalArgumentException("Invalid test configuration.", exception);
        }

        DataRegistry registry = new DataRegistry(Logger.getAnonymousLogger());
        registry.load(configuration);

        return new DataService(
            null,
            registry,
            new ScopeResolver(),
            dataStore,
            new LeaderboardService(dataStore),
            null
        );
    }

    private static OfflinePlayer testPlayer(String name) {
        UUID uniqueId = UUID.nameUUIDFromBytes(name.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        return (OfflinePlayer) Proxy.newProxyInstance(
            OfflinePlayer.class.getClassLoader(),
            new Class<?>[] {OfflinePlayer.class},
            (proxy, method, args) -> switch (method.getName()) {
                case "getUniqueId" -> uniqueId;
                case "getName" -> name;
                case "getPlayer" -> null;
                case "isOnline" -> false;
                case "hashCode" -> uniqueId.hashCode();
                case "equals" -> proxy == args[0];
                case "toString" -> "OfflinePlayer[" + name + "]";
                default -> defaultValue(method.getReturnType());
            }
        );
    }

    private static Object defaultValue(Class<?> type) {
        if (!type.isPrimitive()) {
            return null;
        }
        if (type == boolean.class) {
            return false;
        }
        if (type == byte.class) {
            return (byte) 0;
        }
        if (type == short.class) {
            return (short) 0;
        }
        if (type == int.class) {
            return 0;
        }
        if (type == long.class) {
            return 0L;
        }
        if (type == float.class) {
            return 0F;
        }
        if (type == double.class) {
            return 0D;
        }
        if (type == char.class) {
            return '\0';
        }
        return null;
    }

    private static final class InMemoryDataStore implements DataStore {

        private final Map<String, String> values = new ConcurrentHashMap<>();

        @Override
        public void initialize() {
        }

        @Override
        public Optional<String> findValue(ScopeContext scope, String dataKey) {
            return Optional.ofNullable(values.get(key(scope, dataKey)));
        }

        @Override
        public void saveValueAndSyncLeaderboard(ScopeContext scope, String dataKey, String value, Double leaderboardValue) {
            values.put(key(scope, dataKey), value);
        }

        @Override
        public void deleteValueAndSyncLeaderboard(ScopeContext scope, String dataKey, Double leaderboardValue) {
            values.remove(key(scope, dataKey));
        }

        @Override
        public void saveLeaderboardValue(ScopeContext scope, String dataKey, double numericValue) {
        }

        @Override
        public void deleteScope(ScopeContext scope) {
            values.keySet().removeIf(key -> key.startsWith(scope.type() + "|" + scope.id() + "|"));
        }

        @Override
        public List<LeaderboardEntry> getTopLeaderboard(String dataKey, int limit) {
            return List.of();
        }

        @Override
        public Optional<Integer> findRankInTop(String dataKey, ScopeContext scope, int limit) {
            return Optional.empty();
        }

        @Override
        public int countLeaderboardEntries(String dataKey) {
            return 0;
        }

        @Override
        public List<ScopeContext> getLeaderboardScopes(String dataKey) {
            return List.of();
        }

        @Override
        public List<ScopeContext> getDynamicValueScopes(String dataKey) {
            return List.of();
        }

        @Override
        public void close() {
        }

        void put(ScopeContext scope, String dataKey, String value) {
            values.put(key(scope, dataKey), value);
        }

        private String key(ScopeContext scope, String dataKey) {
            return scope.type() + "|" + scope.id() + "|" + dataKey.toLowerCase();
        }
    }
}
