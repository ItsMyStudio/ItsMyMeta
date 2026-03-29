package studio.itsmy.itsmymeta.meta;

import studio.itsmy.itsmymeta.leaderboard.LeaderboardSettings;
import studio.itsmy.itsmymeta.scope.ScopeType;

public record MetaDefinition(
    String key,
    MetaType metaType,
    boolean decimal,
    ScopeType scopeType,
    Object defaultValue,
    Double minValue,
    Double maxValue,
    LeaderboardSettings leaderboardSettings
) {
}
