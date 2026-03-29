package studio.itsmy.itsmymeta.leaderboard;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.List;
import java.util.Locale;
import studio.itsmy.itsmymeta.meta.MetaDefinition;
import studio.itsmy.itsmymeta.meta.MetaStore;
import studio.itsmy.itsmymeta.meta.MetaType;
import studio.itsmy.itsmymeta.scope.ScopeContext;
public final class LeaderboardService {

    private static final ThreadLocal<DecimalFormat> PERCENT_FORMAT = ThreadLocal.withInitial(
        () -> new DecimalFormat("0.##", DecimalFormatSymbols.getInstance(Locale.US))
    );

    private final MetaStore metaStore;

    public LeaderboardService(MetaStore metaStore) {
        this.metaStore = metaStore;
    }

    public boolean supports(MetaDefinition definition) {
        return definition.leaderboardSettings().enabled() && isNumeric(definition.metaType());
    }

    public void update(MetaDefinition definition, ScopeContext scope, Object value) {
        if (!supports(definition)) {
            return;
        }
        if (!(value instanceof Number number)) {
            throw new IllegalStateException("Leaderboard values must be numeric.");
        }
        metaStore.saveLeaderboardValue(scope, definition.key(), number.doubleValue());
    }

    public LeaderboardEntry getEntry(String metaKey, int position) {
        if (position < 1) {
            throw new IllegalArgumentException("Leaderboard position must be at least 1.");
        }

        List<LeaderboardEntry> entries = metaStore.getTopLeaderboard(metaKey, position);
        if (entries.size() < position) {
            throw new IllegalStateException("No leaderboard entry at position " + position + ".");
        }
        return entries.get(position - 1);
    }

    public LeaderboardPlacement getPlacement(MetaDefinition definition, ScopeContext scope) {
        String metaKey = definition.key();
        int exactRankLimit = definition.leaderboardSettings().exactRankLimit();
        int totalEntries = metaStore.countLeaderboardEntries(metaKey);
        if (totalEntries <= 0) {
            return new LeaderboardPlacement(false, "-", null, "-");
        }

        return metaStore.findRankInTop(metaKey, scope, exactRankLimit)
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

    public int getTotalEntries(String metaKey) {
        return metaStore.countLeaderboardEntries(metaKey);
    }

    private boolean isNumeric(MetaType metaType) {
        return metaType == MetaType.NUMBER;
    }

    private String formatPercent(double value) {
        return PERCENT_FORMAT.get().format(value);
    }
}
