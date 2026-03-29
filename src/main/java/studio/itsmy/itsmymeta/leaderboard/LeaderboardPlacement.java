package studio.itsmy.itsmymeta.leaderboard;

public record LeaderboardPlacement(
    boolean exact,
    String displayRank,
    Integer exactRank,
    String displayPercent
) {
}
