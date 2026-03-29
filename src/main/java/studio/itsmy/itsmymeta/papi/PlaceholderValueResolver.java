package studio.itsmy.itsmymeta.papi;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.regex.Pattern;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginManager;

public final class PlaceholderValueResolver {

    private static final String PLACEHOLDER_API_CLASS = "me.clip.placeholderapi.PlaceholderAPI";
    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("%[A-Za-z0-9_:.\\-]+%");
    private static volatile Method cachedPlaceholderMethod;
    private static volatile boolean placeholderApiUnavailable;

    public String resolve(CommandSender sender, String value) {
        if (!containsPlaceholderMarkers(value)) {
            return value;
        }
        if (!(sender instanceof Player player)) {
            return value;
        }
        return resolve((OfflinePlayer) player, value);
    }

    public String resolve(OfflinePlayer player, String value) {
        if (!containsPlaceholderMarkers(value)) {
            return value;
        }

        Method placeholderMethod = resolvePlaceholderMethod();
        if (placeholderMethod == null) {
            return value;
        }

        try {
            Object resolved = placeholderMethod.invoke(null, player, value);
            return resolved instanceof String string ? string : value;
        } catch (IllegalAccessException | InvocationTargetException exception) {
            return value;
        }
    }

    public boolean containsPlaceholderMarkers(String value) {
        return PLACEHOLDER_PATTERN.matcher(value).find();
    }

    private Method resolvePlaceholderMethod() {
        if (placeholderApiUnavailable) {
            return null;
        }
        Method existingMethod = cachedPlaceholderMethod;
        if (existingMethod != null) {
            return existingMethod;
        }
        if (!isPlaceholderApiAvailable()) {
            placeholderApiUnavailable = true;
            return null;
        }

        synchronized (PlaceholderValueResolver.class) {
            if (cachedPlaceholderMethod != null) {
                return cachedPlaceholderMethod;
            }
            if (placeholderApiUnavailable) {
                return null;
            }

            try {
                Class<?> placeholderApiClass = Class.forName(PLACEHOLDER_API_CLASS);
                cachedPlaceholderMethod = placeholderApiClass.getMethod("setPlaceholders", OfflinePlayer.class, String.class);
                return cachedPlaceholderMethod;
            } catch (ClassNotFoundException | NoSuchMethodException exception) {
                placeholderApiUnavailable = true;
                return null;
            }
        }
    }

    private boolean isPlaceholderApiAvailable() {
        PluginManager pluginManager = Bukkit.getPluginManager();
        return pluginManager != null && pluginManager.getPlugin("PlaceholderAPI") != null;
    }
}
