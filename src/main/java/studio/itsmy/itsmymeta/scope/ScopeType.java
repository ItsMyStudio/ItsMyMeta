package studio.itsmy.itsmymeta.scope;

import java.util.Locale;

public enum ScopeType {
    GLOBAL,
    PLAYER,
    WORLD,
    FACTION,
    LANDS,
    SUPERIORSKYBLOCK;

    public static ScopeType fromConfigValue(String value) {
        return ScopeType.valueOf(value.trim().toUpperCase(Locale.ROOT));
    }
}
