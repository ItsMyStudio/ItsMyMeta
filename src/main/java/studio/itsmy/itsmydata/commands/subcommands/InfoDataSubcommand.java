package studio.itsmy.itsmydata.commands.subcommands;

import java.util.List;
import org.bukkit.command.CommandSender;
import studio.itsmy.itsmydata.message.MessageService;
import studio.itsmy.itsmydata.data.DataDefinition;
import studio.itsmy.itsmydata.commands.DataCommandTargetResolver;
import studio.itsmy.itsmydata.data.DataService;

public final class InfoDataSubcommand extends AbstractDataSubcommand {

    public InfoDataSubcommand(DataService dataService, MessageService messages) {
        super(dataService, messages);
    }

    @Override
    public String name() {
        return "info";
    }

    @Override
    public String permission() {
        return "itsmydata.command.info";
    }

    @Override
    public String usage() {
        return "info <dataKey> [target]";
    }

    @Override
    public String descriptionKey() {
        return "messages.descriptions.info";
    }

    @Override
    public boolean execute(CommandSender sender, String label, String[] args) {
        if (!requireDataKey(sender, label, args, usage())) {
            return true;
        }

        String dataKey = args[1];
        DataDefinition definition = dataService.getDefinition(dataKey);
        DataCommandTargetResolver.ResolvedDataTarget target = resolveReadTarget(sender, definition, args);
        messages.sendList(
            sender,
            "messages.commands.info",
            messages.placeholder("data_key", dataKey),
            messages.placeholder("data_type", definition.dataType().name().toLowerCase()),
            messages.placeholder("scope", definition.scopeType().name().toLowerCase()),
            messages.placeholder("default", getDefaultValue(sender, dataKey, target)),
            messages.placeholder("min", definition.minValue() == null ? messages.raw("messages.common.none") : definition.minValue()),
            messages.placeholder("max", definition.maxValue() == null ? messages.raw("messages.common.none") : definition.maxValue())
        );
        return true;
    }
}
