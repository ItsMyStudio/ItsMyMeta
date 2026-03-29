package studio.itsmy.itsmymeta.leaderboard;

import studio.itsmy.itsmymeta.scope.ScopeType;

public record LeaderboardEntry(
    int rank,
    String metaKey,
    ScopeType scopeType,
    String scopeId,
    double numericValue
) {
}
