package studio.itsmy.itsmydata.leaderboard;

public record LeaderboardSettings(
    boolean enabled,
    int exactRankLimit,
    int dynamicRefreshMinutes
) {

    public static final LeaderboardSettings DISABLED = new LeaderboardSettings(false, 1000, 0);
}
