package studio.itsmy.itsmydata.commands.subcommands;

import java.util.logging.Logger;
import org.bukkit.command.CommandSender;
import studio.itsmy.itsmydata.message.MessageService;
import studio.itsmy.itsmydata.data.DataDefinition;
import studio.itsmy.itsmydata.commands.DataCommandTargetResolver;
import studio.itsmy.itsmydata.data.DataService;
import studio.itsmy.itsmydata.scope.ResolvedScope;
import studio.itsmy.itsmydata.task.TaskDispatcher;

public final class InfoDataSubcommand extends AbstractDataSubcommand {

    public InfoDataSubcommand(TaskDispatcher taskDispatcher, Logger logger, DataService dataService, MessageService messages) {
        super(taskDispatcher, logger, dataService, messages);
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
        return "info <dataKey> <target>";
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
        if (isTargetRequired(definition) && args.length < 3) {
            return requireTarget(sender, label, usage());
        }
        DataCommandTargetResolver.ResolvedDataTarget target = resolveReadTarget(sender, definition, args);
        ResolvedScope resolvedScope = target.resolvedScope();
        messages.sendList(
            sender,
            "messages.commands.info",
            messages.placeholder("data_key", dataKey),
            messages.placeholder("data_type", definition.dataType().name().toLowerCase()),
            messages.placeholder("scope", definition.scopeType().name().toLowerCase()),
            messages.placeholder("default", dataService.getDefaultValue(resolvedScope.player(), dataKey)),
            messages.placeholder("min", definition.minValue() == null ? messages.raw("messages.common.none") : definition.minValue()),
            messages.placeholder("max", definition.maxValue() == null ? messages.raw("messages.common.none") : definition.maxValue())
        );
        return true;
    }
}
