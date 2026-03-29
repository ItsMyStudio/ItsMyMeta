package studio.itsmy.itsmymeta.message;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

public final class MessageService {

    private final JavaPlugin plugin;
    private final MiniMessage miniMessage;
    private YamlConfiguration languageConfiguration;
    private Component prefixComponent;

    public MessageService(JavaPlugin plugin) {
        this.plugin = plugin;
        this.miniMessage = MiniMessage.miniMessage();
    }

    public void load(String fileName) {
        plugin.saveResource(fileName, false);
        File languageFile = new File(plugin.getDataFolder(), fileName);
        this.languageConfiguration = YamlConfiguration.loadConfiguration(languageFile);
        String prefix = languageConfiguration.getString("prefix", "");
        this.prefixComponent = prefix.isBlank() ? Component.empty() : miniMessage.deserialize(prefix);
    }

    public void send(CommandSender sender, String path, TagResolver... resolvers) {
        sender.sendMessage(render(path, resolvers));
    }

    public void sendList(CommandSender sender, String path, TagResolver... resolvers) {
        List<String> lines = languageConfiguration.getStringList(path);
        if (lines.isEmpty()) {
            send(sender, path, resolvers);
            return;
        }

        for (String line : lines) {
            sender.sendMessage(renderTemplate(line, resolvers));
        }
    }

    public String raw(String path) {
        String value = languageConfiguration.getString(path);
        if (value == null) {
            throw new IllegalStateException("Missing lang.yml message path: " + path);
        }
        return value;
    }

    public void info(String path, TagResolver... resolvers) {
        plugin.getLogger().info(toPlainText(render(path, resolvers)));
    }

    public TagResolver placeholder(String key, Object value) {
        return Placeholder.unparsed(key, String.valueOf(value));
    }

    private Component render(String path, TagResolver... resolvers) {
        return miniMessage.deserialize(raw(path), mergeResolvers(resolvers));
    }

    private Component renderTemplate(String content, TagResolver... resolvers) {
        return miniMessage.deserialize(content, mergeResolvers(resolvers));
    }

    private TagResolver mergeResolvers(TagResolver... resolvers) {
        List<TagResolver> merged = new ArrayList<>(resolvers.length + 1);
        merged.add(Placeholder.component("prefix", prefixComponent == null ? Component.empty() : prefixComponent));
        for (TagResolver resolver : resolvers) {
            merged.add(resolver);
        }
        return TagResolver.resolver(merged);
    }

    private String toPlainText(Component component) {
        return PlainTextComponentSerializer.plainText().serialize(component);
    }
}
