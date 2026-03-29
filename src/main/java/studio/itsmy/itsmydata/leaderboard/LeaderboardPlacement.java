package studio.itsmy.itsmydata.leaderboard;

public record LeaderboardPlacement(
    boolean exact,
    String displayRank,
    Integer exactRank,
    String displayPercent
) {
}
