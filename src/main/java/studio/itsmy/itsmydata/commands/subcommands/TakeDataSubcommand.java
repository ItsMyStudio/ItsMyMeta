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

public final class TakeDataSubcommand extends AbstractDataSubcommand {

    public TakeDataSubcommand(TaskDispatcher taskDispatcher, Logger logger, DataService dataService, MessageService messages) {
        super(taskDispatcher, logger, dataService, messages);
    }

    @Override
    public String name() {
        return "take";
    }

    @Override
    public String permission() {
        return "itsmydata.command.take";
    }

    @Override
    public String usage() {
        return "take <dataKey> <target> <amount>";
    }

    @Override
    public String descriptionKey() {
        return "messages.descriptions.take";
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

        String amount = String.join(" ", Arrays.copyOfRange(args, target.nextArgIndex(), args.length));
        ResolvedScope resolvedScope = target.resolvedScope();
        return completeAsync(sender, dataService.takeValueAsync(resolvedScope, definition, amount), updatedValue -> messages.send(
            sender,
            "messages.commands.take.success",
            messages.placeholder("data_key", dataKey),
            messages.placeholder("target", resolvedScope.displayTarget()),
            messages.placeholder("amount", amount),
            messages.placeholder("value", updatedValue)
        ));
    }
}
