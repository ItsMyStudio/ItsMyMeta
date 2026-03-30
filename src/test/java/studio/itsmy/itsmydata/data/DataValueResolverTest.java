package studio.itsmy.itsmydata.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTimeout;

import java.time.Duration;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import studio.itsmy.itsmydata.leaderboard.LeaderboardSettings;
import studio.itsmy.itsmydata.scope.ScopeType;

class DataValueResolverTest {

    private final DataValueResolver resolver = new DataValueResolver();

    @Test
    void resolveStoredValueFallsBackToResolvedDefault() {
        DataDefinition definition = new DataDefinition(
            "coins",
            DataType.NUMBER,
            false,
            ScopeType.PLAYER,
            7,
            null,
            null,
            LeaderboardSettings.DISABLED
        );

        assertEquals(7, resolver.resolveStoredValue(null, definition, Optional.empty()));
    }

    @Test
    void resolveStoredValueParsesNumericExpressions() {
        DataDefinition definition = new DataDefinition(
            "coins",
            DataType.NUMBER,
            false,
            ScopeType.PLAYER,
            0,
            null,
            null,
            LeaderboardSettings.DISABLED
        );

        assertEquals(14, resolver.resolveStoredValue(null, definition, Optional.of("2 + 3 * 4")));
    }

    @Test
    void castNumberAppliesBounds() {
        DataDefinition definition = new DataDefinition(
            "coins",
            DataType.NUMBER,
            false,
            ScopeType.PLAYER,
            0,
            0D,
            10D,
            LeaderboardSettings.DISABLED
        );

        assertEquals(0, resolver.castNumber(-5, definition));
        assertEquals(10, resolver.castNumber(25, definition));
    }

    @Test
    void castNumberRejectsFractionalWholeNumbers() {
        DataDefinition definition = new DataDefinition(
            "coins",
            DataType.NUMBER,
            false,
            ScopeType.PLAYER,
            0,
            null,
            null,
            LeaderboardSettings.DISABLED
        );

        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> resolver.castNumber(1.5D, definition)
        );

        assertEquals("Data 'coins' requires a whole number.", exception.getMessage());
    }

    @Test
    void hotPathResolutionCompletesWithinReasonableTime() {
        DataDefinition definition = new DataDefinition(
            "coins",
            DataType.NUMBER,
            false,
            ScopeType.PLAYER,
            0,
            0D,
            999_999D,
            LeaderboardSettings.DISABLED
        );

        assertTimeout(Duration.ofSeconds(5), () -> {
            for (int index = 0; index < 100_000; index++) {
                resolver.resolveStoredValue(null, definition, Optional.of("250"));
            }
        });
    }
}
