package studio.itsmy.itsmydata.leaderboard;

import studio.itsmy.itsmydata.scope.ScopeType;

public record LeaderboardEntry(
    int rank,
    String dataKey,
    ScopeType scopeType,
    String scopeId,
    double numericValue
) {
}
