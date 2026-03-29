package studio.itsmy.itsmydata.commands.subcommands;

import org.bukkit.command.CommandSender;
import studio.itsmy.itsmydata.data.DataDefinition;
import studio.itsmy.itsmydata.message.MessageService;
import studio.itsmy.itsmydata.data.DataService;

public final class GetDataSubcommand extends AbstractDataSubcommand {

    public GetDataSubcommand(DataService dataService, MessageService messages) {
        super(dataService, messages);
    }

    @Override
    public String name() {
        return "get";
    }

    @Override
    public String permission() {
        return "itsmydata.command.get";
    }

    @Override
    public String usage() {
        return "get <dataKey> [target]";
    }

    @Override
    public String descriptionKey() {
        return "messages.descriptions.get";
    }

    @Override
    public boolean execute(CommandSender sender, String label, String[] args) {
        if (!requireDataKey(sender, label, args, usage())) {
            return true;
        }

        String dataKey = args[1];
        DataDefinition definition = dataService.getDefinition(dataKey);
        var target = resolveReadTarget(sender, definition, args);
        Object value = getValue(sender, dataKey, target);
        messages.send(
            sender,
            "messages.commands.get.value",
            messages.placeholder("data_key", dataKey),
            messages.placeholder("target", target.resolvedScope().displayTarget()),
            messages.placeholder("value", value)
        );
        return true;
    }
}
