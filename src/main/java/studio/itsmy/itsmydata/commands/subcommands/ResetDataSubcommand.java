package studio.itsmy.itsmydata.commands.subcommands;

import java.util.logging.Logger;
import org.bukkit.command.CommandSender;
import studio.itsmy.itsmydata.data.DataDefinition;
import studio.itsmy.itsmydata.message.MessageService;
import studio.itsmy.itsmydata.data.DataService;
import studio.itsmy.itsmydata.scope.ResolvedScope;
import studio.itsmy.itsmydata.task.TaskDispatcher;

public final class ResetDataSubcommand extends AbstractDataSubcommand {

    public ResetDataSubcommand(TaskDispatcher taskDispatcher, Logger logger, DataService dataService, MessageService messages) {
        super(taskDispatcher, logger, dataService, messages);
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
        ResolvedScope resolvedScope = target.resolvedScope();
        return completeAsync(sender, dataService.resetValueAsync(resolvedScope, definition), resetValue -> messages.send(
            sender,
            "messages.commands.reset.success",
            messages.placeholder("data_key", dataKey),
            messages.placeholder("target", resolvedScope.displayTarget()),
            messages.placeholder("value", resetValue)
        ));
    }
}
