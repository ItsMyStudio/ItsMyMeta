package studio.itsmy.itsmydata.data;

import studio.itsmy.itsmydata.leaderboard.LeaderboardSettings;
import studio.itsmy.itsmydata.scope.ScopeType;

public record DataDefinition(
    String key,
    DataType dataType,
    boolean decimal,
    ScopeType scopeType,
    Object defaultValue,
    Double minValue,
    Double maxValue,
    LeaderboardSettings leaderboardSettings
) {
}
