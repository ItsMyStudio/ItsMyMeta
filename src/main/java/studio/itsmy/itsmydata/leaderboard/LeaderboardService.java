package studio.itsmy.itsmydata.leaderboard;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.List;
import java.util.Locale;
import studio.itsmy.itsmydata.data.DataDefinition;
import studio.itsmy.itsmydata.data.DataStore;
import studio.itsmy.itsmydata.data.DataType;
import studio.itsmy.itsmydata.scope.ScopeContext;
public final class LeaderboardService {

    private static final ThreadLocal<DecimalFormat> PERCENT_FORMAT = ThreadLocal.withInitial(
        () -> new DecimalFormat("0.##", DecimalFormatSymbols.getInstance(Locale.US))
    );

    private final DataStore dataStore;

    public LeaderboardService(DataStore dataStore) {
        this.dataStore = dataStore;
    }

    public boolean supports(DataDefinition definition) {
        return definition.leaderboardSettings().enabled() && isNumeric(definition.dataType());
    }

    public void update(DataDefinition definition, ScopeContext scope, Object value) {
        if (!supports(definition)) {
            return;
        }
        if (!(value instanceof Number number)) {
            throw new IllegalStateException("Leaderboard values must be numeric.");
        }
        dataStore.saveLeaderboardValue(scope, definition.key(), number.doubleValue());
    }

    public LeaderboardEntry getEntry(String dataKey, int position) {
        if (position < 1) {
            throw new IllegalArgumentException("Leaderboard position must be at least 1.");
        }

        List<LeaderboardEntry> entries = dataStore.getTopLeaderboard(dataKey, position);
        if (entries.size() < position) {
            throw new IllegalStateException("No leaderboard entry at position " + position + ".");
        }
        return entries.get(position - 1);
    }

    public LeaderboardPlacement getPlacement(DataDefinition definition, ScopeContext scope) {
        String dataKey = definition.key();
        int exactRankLimit = definition.leaderboardSettings().exactRankLimit();
        int totalEntries = dataStore.countLeaderboardEntries(dataKey);
        if (totalEntries <= 0) {
            return new LeaderboardPlacement(false, "-", null, "-");
        }

        return dataStore.findRankInTop(dataKey, scope, exactRankLimit)
            .map(rank -> new LeaderboardPlacement(
                true,
                "#" + rank,
                rank,
                formatPercent((rank * 100D) / totalEntries) + "%"
            ))
            .orElse(new LeaderboardPlacement(
                false,
                exactRankLimit + "+",
                null,
                ">" + formatPercent((exactRankLimit * 100D) / totalEntries) + "%"
            ));
    }

    public int getTotalEntries(String dataKey) {
        return dataStore.countLeaderboardEntries(dataKey);
    }

    private boolean isNumeric(DataType dataType) {
        return dataType == DataType.NUMBER;
    }

    private String formatPercent(double value) {
        return PERCENT_FORMAT.get().format(value);
    }
}
