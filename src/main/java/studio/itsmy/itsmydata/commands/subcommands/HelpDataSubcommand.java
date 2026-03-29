package studio.itsmy.itsmydata.commands.subcommands;

import java.util.Map;
import org.bukkit.command.CommandSender;
import studio.itsmy.itsmydata.message.MessageService;

public final class HelpDataSubcommand implements DataSubcommand {

    private final Map<String, DataSubcommand> subcommands;
    private final MessageService messages;

    public HelpDataSubcommand(Map<String, DataSubcommand> subcommands, MessageService messages) {
        this.subcommands = subcommands;
        this.messages = messages;
    }

    @Override
    public String name() {
        return "help";
    }

    @Override
    public String permission() {
        return "itsmydata.command.help";
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
        for (DataSubcommand subcommand : subcommands.values()) {
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
