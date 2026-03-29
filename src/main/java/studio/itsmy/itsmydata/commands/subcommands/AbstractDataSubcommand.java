package studio.itsmy.itsmydata.commands.subcommands;

import org.bukkit.command.CommandSender;
import studio.itsmy.itsmydata.commands.DataCommandTargetResolver;
import studio.itsmy.itsmydata.data.DataDefinition;
import studio.itsmy.itsmydata.message.MessageService;
import studio.itsmy.itsmydata.data.DataService;
import studio.itsmy.itsmydata.scope.ResolvedScope;

public abstract class AbstractDataSubcommand implements DataSubcommand {

    protected final MessageService messages;
    protected final DataService dataService;
    private final DataCommandTargetResolver targetResolver;

    protected AbstractDataSubcommand(DataService dataService, MessageService messages) {
        this.dataService = dataService;
        this.messages = messages;
        this.targetResolver = new DataCommandTargetResolver();
    }

    protected boolean requireDataKey(CommandSender sender, String label, String[] args, String usage) {
        if (args.length < 2) {
            sendIncorrectCommand(sender, label, usage);
            return false;
        }
        return true;
    }

    protected boolean requireValue(CommandSender sender, String label, String[] args, String usage) {
        if (args.length < 3) {
            sendIncorrectCommand(sender, label, usage);
            return false;
        }
        return true;
    }

    protected boolean requireTarget(CommandSender sender, String label, String usage) {
        sendIncorrectCommand(sender, label, usage);
        return true;
    }

    protected boolean requireValue(CommandSender sender, String label, String usage) {
        sendIncorrectCommand(sender, label, usage);
        return true;
    }

    protected DataCommandTargetResolver.ResolvedDataTarget resolveReadTarget(CommandSender sender, DataDefinition definition, String[] args) {
        return targetResolver.resolveForRead(sender, definition, args, 2);
    }

    protected DataCommandTargetResolver.ResolvedDataTarget resolveWriteTarget(CommandSender sender, DataDefinition definition, String[] args) {
        return targetResolver.resolveForWrite(sender, definition, args, 2);
    }

    protected Object getValue(CommandSender sender, String dataKey, DataCommandTargetResolver.ResolvedDataTarget target) {
        ResolvedScope resolvedScope = target.resolvedScope();
        if (resolvedScope.placeholderPlayer() != null) {
            return dataService.getValue(resolvedScope.placeholderPlayer(), resolvedScope.scope(), dataKey);
        }
        return dataService.getValue(sender, resolvedScope.scope(), dataKey);
    }

    protected Object getDefaultValue(CommandSender sender, String dataKey, DataCommandTargetResolver.ResolvedDataTarget target) {
        ResolvedScope resolvedScope = target.resolvedScope();
        if (resolvedScope.placeholderPlayer() != null) {
            return dataService.getDefaultValue(resolvedScope.placeholderPlayer(), dataKey, resolvedScope.scope());
        }
        return dataService.getDefaultValue(sender, dataKey, resolvedScope.scope());
    }

    protected Object setValue(CommandSender sender, String dataKey, Object value, DataCommandTargetResolver.ResolvedDataTarget target) {
        ResolvedScope resolvedScope = target.resolvedScope();
        if (resolvedScope.placeholderPlayer() != null) {
            return dataService.setValue(resolvedScope.placeholderPlayer(), resolvedScope.scope(), dataKey, value);
        }
        return dataService.setValue(sender, resolvedScope.scope(), dataKey, value);
    }

    protected Object resetValue(CommandSender sender, String dataKey, DataCommandTargetResolver.ResolvedDataTarget target) {
        ResolvedScope resolvedScope = target.resolvedScope();
        if (resolvedScope.placeholderPlayer() != null) {
            return dataService.resetValue(resolvedScope.placeholderPlayer(), resolvedScope.scope(), dataKey);
        }
        return dataService.resetValue(sender, resolvedScope.scope(), dataKey);
    }

    protected Object giveValue(CommandSender sender, String dataKey, String amount, DataCommandTargetResolver.ResolvedDataTarget target) {
        ResolvedScope resolvedScope = target.resolvedScope();
        if (resolvedScope.placeholderPlayer() != null) {
            return dataService.giveValue(resolvedScope.placeholderPlayer(), resolvedScope.scope(), dataKey, amount);
        }
        return dataService.giveValue(sender, resolvedScope.scope(), dataKey, amount);
    }

    protected Object takeValue(CommandSender sender, String dataKey, String amount, DataCommandTargetResolver.ResolvedDataTarget target) {
        ResolvedScope resolvedScope = target.resolvedScope();
        if (resolvedScope.placeholderPlayer() != null) {
            return dataService.takeValue(resolvedScope.placeholderPlayer(), resolvedScope.scope(), dataKey, amount);
        }
        return dataService.takeValue(sender, resolvedScope.scope(), dataKey, amount);
    }

    private void sendIncorrectCommand(CommandSender sender, String label, String usage) {
        messages.send(
            sender,
            "messages.errors.incorrect-command",
            messages.placeholder("label", label),
            messages.placeholder("usage", usage)
        );
    }
}
