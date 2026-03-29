package studio.itsmy.itsmymeta.commands.subcommands;

import java.util.Map;
import org.bukkit.command.CommandSender;
import studio.itsmy.itsmymeta.message.MessageService;

public final class HelpMetaSubcommand implements MetaSubcommand {

    private final Map<String, MetaSubcommand> subcommands;
    private final MessageService messages;

    public HelpMetaSubcommand(Map<String, MetaSubcommand> subcommands, MessageService messages) {
        this.subcommands = subcommands;
        this.messages = messages;
    }

    @Override
    public String name() {
        return "help";
    }

    @Override
    public String permission() {
        return "itsmymeta.command.help";
    }

    @Override
    public String usage() {
        return "help";
    }

    @Override
    public String descriptionKey() {
        return "messages.descriptions.help";
    }

    @Override
    public boolean execute(CommandSender sender, String label, String[] args) {
        messages.send(sender, "messages.commands.help.header");
        for (MetaSubcommand subcommand : subcommands.values()) {
            if (!sender.hasPermission(subcommand.permission())) {
                continue;
            }
            messages.send(
                sender,
                "messages.commands.help.entry",
                messages.placeholder("label", label),
                messages.placeholder("usage", subcommand.usage()),
                messages.placeholder("description", messages.raw(subcommand.descriptionKey()))
            );
        }
        return true;
    }
}
