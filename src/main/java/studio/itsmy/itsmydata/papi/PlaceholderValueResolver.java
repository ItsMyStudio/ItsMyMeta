package studio.itsmy.itsmydata.papi;

import java.util.regex.Pattern;
import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.PluginManager;

public final class PlaceholderValueResolver {

    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("%[A-Za-z0-9_:.\\-]+%");

    public String resolve(OfflinePlayer player, String value) {
        if (!containsPlaceholderMarkers(value)) {
            return value;
        }
        if (!isPlaceholderApiAvailable()) {
            return value;
        }

        try {
            return PlaceholderAPI.setPlaceholders(player, value);
        } catch (RuntimeException exception) {
            return value;
        }
    }

    public boolean containsPlaceholderMarkers(Object value) {
        if (!(value instanceof String stringValue)) {
            return false;
        }

        return PLACEHOLDER_PATTERN.matcher(stringValue).find();
    }

    private boolean isPlaceholderApiAvailable() {
        PluginManager pluginManager = Bukkit.getPluginManager();
        return pluginManager.getPlugin("PlaceholderAPI") != null;
    }
}
