package studio.itsmy.itsmydata.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import studio.itsmy.itsmydata.leaderboard.LeaderboardSettings;
import studio.itsmy.itsmydata.scope.ScopeType;

class DataDefinitionTest {

    @Test
    void missingIntegerDefaultFallsBackToZero() {
        DataDefinition definition = new DataDefinition(
            "coins",
            DataType.NUMBER,
            false,
            ScopeType.PLAYER,
            null,
            null,
            null,
            LeaderboardSettings.DISABLED
        );

        assertEquals(0, definition.defaultValue());
    }

    @Test
    void missingDecimalDefaultFallsBackToZeroDouble() {
        DataDefinition definition = new DataDefinition(
            "balance",
            DataType.NUMBER,
            true,
            ScopeType.PLAYER,
            null,
            null,
            null,
            LeaderboardSettings.DISABLED
        );

        assertInstanceOf(Double.class, definition.defaultValue());
        assertEquals(0D, definition.defaultValue());
    }

    @Test
    void missingStringDefaultFallsBackToEmptyString() {
        DataDefinition definition = new DataDefinition(
            "title",
            DataType.STRING,
            false,
            ScopeType.PLAYER,
            null,
            null,
            null,
            LeaderboardSettings.DISABLED
        );

        assertEquals("", definition.defaultValue());
    }

    @Test
    void stringDefaultIsNormalizedToString() {
        DataDefinition definition = new DataDefinition(
            "title",
            DataType.STRING,
            false,
            ScopeType.PLAYER,
            123,
            null,
            null,
            LeaderboardSettings.DISABLED
        );

        assertEquals("123", definition.defaultValue());
    }

    @Test
    void invalidStringBoundsAreRejected() {
        IllegalStateException exception = assertThrows(
            IllegalStateException.class,
            () -> new DataDefinition(
                "title",
                DataType.STRING,
                false,
                ScopeType.GLOBAL,
                "hello",
                0D,
                null,
                LeaderboardSettings.DISABLED
            )
        );

        assertEquals("Data 'title' cannot define min/max when it is not numeric.", exception.getMessage());
    }

    @Test
    void invalidWholeNumberDefaultIsRejected() {
        IllegalStateException exception = assertThrows(
            IllegalStateException.class,
            () -> new DataDefinition(
                "coins",
                DataType.NUMBER,
                false,
                ScopeType.PLAYER,
                1.5D,
                null,
                null,
                LeaderboardSettings.DISABLED
            )
        );
        assertEquals("Data 'coins' has an invalid default value.", exception.getMessage());
    }

    @Test
    void placeholderNumericDefaultIsAccepted() {
        DataDefinition definition = new DataDefinition(
            "coins",
            DataType.NUMBER,
            false,
            ScopeType.PLAYER,
            "%player_level%",
            null,
            null,
            LeaderboardSettings.DISABLED
        );

        assertEquals("%player_level%", definition.defaultValue());
    }
}
