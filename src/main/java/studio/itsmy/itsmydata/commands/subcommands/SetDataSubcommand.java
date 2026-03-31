package studio.itsmy.itsmydata.commands.subcommands;

import java.util.Arrays;
import java.util.logging.Logger;
import org.bukkit.command.CommandSender;
import studio.itsmy.itsmydata.message.MessageService;
import studio.itsmy.itsmydata.data.DataDefinition;
import studio.itsmy.itsmydata.data.DataService;
import studio.itsmy.itsmydata.scope.ResolvedScope;
import studio.itsmy.itsmydata.scope.ScopeType;
import studio.itsmy.itsmydata.task.TaskDispatcher;

public final class SetDataSubcommand extends AbstractDataSubcommand {

    public SetDataSubcommand(TaskDispatcher taskDispatcher, Logger logger, DataService dataService, MessageService messages) {
        super(taskDispatcher, logger, dataService, messages);
    }

    @Override
    public String name() {
        return "set";
    }

    @Override
    public String permission() {
        return "itsmydata.command.set";
    }

    @Override
    public String usage() {
        return "set <dataKey> <target> <value>";
    }

    @Override
    public String descriptionKey() {
        return "messages.descriptions.set";
    }

    @Override
    public boolean execute(CommandSender sender, String label, String[] args) {
        if (!requireDataKey(sender, label, args, usage())) {
            return true;
        }
        if (!requireValue(sender, label, args, usage())) {
            return true;
        }

        String dataKey = args[1];
        DataDefinition definition = dataService.getDefinition(dataKey);
        if (definition.scopeType() != ScopeType.GLOBAL && args.length < 4) {
            return requireTarget(sender, label, usage());
        }

        var target = resolveWriteTarget(sender, definition, args);
        if (args.length <= target.nextArgIndex()) {
            return requireValue(sender, label, usage());
        }

        String value = String.join(" ", Arrays.copyOfRange(args, target.nextArgIndex(), args.length));
        ResolvedScope resolvedScope = target.resolvedScope();
        return completeAsync(sender, dataService.setValueAsync(resolvedScope, definition, value), updatedValue -> messages.send(
            sender,
            "messages.commands.set.success",
            messages.placeholder("data_key", dataKey),
            messages.placeholder("target", resolvedScope.displayTarget()),
            messages.placeholder("value", updatedValue)
        ));
    }
}
