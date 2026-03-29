package studio.itsmy.itsmymeta.meta;

import java.util.List;
import java.util.Optional;
import studio.itsmy.itsmymeta.leaderboard.LeaderboardEntry;
import studio.itsmy.itsmymeta.scope.ScopeContext;

public interface MetaStore extends AutoCloseable {

    void initialize();

    Optional<String> findValue(ScopeContext scope, String metaKey);

    void saveValueAndSyncLeaderboard(ScopeContext scope, String metaKey, String value, Double leaderboardValue);

    void deleteValueAndSyncLeaderboard(ScopeContext scope, String metaKey, Double leaderboardValue);

    void saveLeaderboardValue(ScopeContext scope, String metaKey, double numericValue);

    void deleteScope(ScopeContext scope);

    List<LeaderboardEntry> getTopLeaderboard(String metaKey, int limit);

    Optional<Integer> findRankInTop(String metaKey, ScopeContext scope, int limit);

    int countLeaderboardEntries(String metaKey);

    List<ScopeContext> getLeaderboardScopes(String metaKey);

    List<ScopeContext> getDynamicValueScopes(String metaKey);

    @Override
    void close();
}
