package studio.itsmy.itsmydata.data;

import java.util.List;
import java.util.Optional;
import studio.itsmy.itsmydata.leaderboard.LeaderboardEntry;
import studio.itsmy.itsmydata.scope.ScopeContext;

public interface DataStore extends AutoCloseable {

    void initialize();

    Optional<String> findValue(ScopeContext scope, String dataKey);

    void saveValueAndSyncLeaderboard(ScopeContext scope, String dataKey, String value, Double leaderboardValue);

    void deleteValueAndSyncLeaderboard(ScopeContext scope, String dataKey, Double leaderboardValue);

    void saveLeaderboardValue(ScopeContext scope, String dataKey, double numericValue);

    void deleteScope(ScopeContext scope);

    List<LeaderboardEntry> getTopLeaderboard(String dataKey, int limit);

    Optional<Integer> findRankInTop(String dataKey, ScopeContext scope, int limit);

    int countLeaderboardEntries(String dataKey);

    List<ScopeContext> getLeaderboardScopes(String dataKey);

    List<ScopeContext> getDynamicValueScopes(String dataKey);

    @Override
    void close();
}
