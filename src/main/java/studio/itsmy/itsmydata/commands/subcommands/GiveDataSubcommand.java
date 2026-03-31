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

public final class GiveDataSubcommand extends AbstractDataSubcommand {

    public GiveDataSubcommand(TaskDispatcher taskDispatcher, Logger logger, DataService dataService, MessageService messages) {
        super(taskDispatcher, logger, dataService, messages);
    }

    @Override
    public String name() {
        return "give";
    }

    @Override
    public String permission() {
        return "itsmydata.command.give";
    }

    @Override
    public String usage() {
        return "give <dataKey> <target> <amount>";
    }

    @Override
    public String descriptionKey() {
        return "messages.descriptions.give";
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
        return completeAsync(sender, dataService.giveValueAsync(resolvedScope, definition, amount), updatedValue -> messages.send(
            sender,
            "messages.commands.give.success",
            messages.placeholder("data_key", dataKey),
            messages.placeholder("target", resolvedScope.displayTarget()),
            messages.placeholder("amount", amount),
            messages.placeholder("value", updatedValue)
        ));
    }
}
