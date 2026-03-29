package studio.itsmy.itsmydata.commands.subcommands;

import org.bukkit.command.CommandSender;
import studio.itsmy.itsmydata.data.DataDefinition;
import studio.itsmy.itsmydata.message.MessageService;
import studio.itsmy.itsmydata.data.DataService;

public final class ResetDataSubcommand extends AbstractDataSubcommand {

    public ResetDataSubcommand(DataService dataService, MessageService messages) {
        super(dataService, messages);
    }

    @Override
    public String name() {
        return "reset";
    }

    @Override
    public String permission() {
        return "itsmydata.command.reset";
    }

    @Override
    public String usage() {
        return "reset <dataKey> [target]";
    }

    @Override
    public String descriptionKey() {
        return "messages.descriptions.reset";
    }

    @Override
    public boolean execute(CommandSender sender, String label, String[] args) {
        if (!requireDataKey(sender, label, args, usage())) {
            return true;
        }

        String dataKey = args[1];
        DataDefinition definition = dataService.getDefinition(dataKey);
        var target = resolveReadTarget(sender, definition, args);
        Object resetValue = resetValue(sender, dataKey, target);
        messages.send(
            sender,
            "messages.commands.reset.success",
            messages.placeholder("data_key", dataKey),
            messages.placeholder("target", target.resolvedScope().displayTarget()),
            messages.placeholder("value", resetValue)
        );
        return true;
    }
}
