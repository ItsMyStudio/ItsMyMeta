package studio.itsmy.itsmydata.commands.subcommands;

import java.util.logging.Logger;
import org.bukkit.command.CommandSender;
import studio.itsmy.itsmydata.data.DataDefinition;
import studio.itsmy.itsmydata.message.MessageService;
import studio.itsmy.itsmydata.data.DataService;
import studio.itsmy.itsmydata.scope.ResolvedScope;
import studio.itsmy.itsmydata.task.TaskDispatcher;

public final class GetDataSubcommand extends AbstractDataSubcommand {

    public GetDataSubcommand(TaskDispatcher taskDispatcher, Logger logger, DataService dataService, MessageService messages) {
        super(taskDispatcher, logger, dataService, messages);
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
        return "get <dataKey> <target>";
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
        if (isTargetRequired(definition) && args.length < 3) {
            return requireTarget(sender, label, usage());
        }
        var target = resolveReadTarget(sender, definition, args);
        ResolvedScope resolvedScope = target.resolvedScope();
        return completeAsync(sender, dataService.getValueAsync(resolvedScope, definition), value -> messages.send(
            sender,
            "messages.commands.get.value",
            messages.placeholder("data_key", dataKey),
            messages.placeholder("target", resolvedScope.displayTarget()),
            messages.placeholder("value", value)
        ));
    }
}
