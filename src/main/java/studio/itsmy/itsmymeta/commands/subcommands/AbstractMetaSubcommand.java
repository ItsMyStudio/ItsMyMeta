package studio.itsmy.itsmymeta.commands.subcommands;

import org.bukkit.command.CommandSender;
import studio.itsmy.itsmymeta.commands.MetaCommandTargetResolver;
import studio.itsmy.itsmymeta.meta.MetaDefinition;
import studio.itsmy.itsmymeta.message.MessageService;
import studio.itsmy.itsmymeta.meta.MetaService;
import studio.itsmy.itsmymeta.scope.ResolvedScope;

public abstract class AbstractMetaSubcommand implements MetaSubcommand {

    protected final MessageService messages;
    protected final MetaService metaService;
    private final MetaCommandTargetResolver targetResolver;

    protected AbstractMetaSubcommand(MetaService metaService, MessageService messages) {
        this.metaService = metaService;
        this.messages = messages;
        this.targetResolver = new MetaCommandTargetResolver();
    }

    protected boolean requireMetaKey(CommandSender sender, String label, String[] args, String usage) {
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

    protected MetaCommandTargetResolver.ResolvedMetaTarget resolveReadTarget(CommandSender sender, MetaDefinition definition, String[] args) {
        return targetResolver.resolveForRead(sender, definition, args, 2);
    }

    protected MetaCommandTargetResolver.ResolvedMetaTarget resolveWriteTarget(CommandSender sender, MetaDefinition definition, String[] args) {
        return targetResolver.resolveForWrite(sender, definition, args, 2);
    }

    protected Object getValue(CommandSender sender, String metaKey, MetaCommandTargetResolver.ResolvedMetaTarget target) {
        ResolvedScope resolvedScope = target.resolvedScope();
        if (resolvedScope.placeholderPlayer() != null) {
            return metaService.getValue(resolvedScope.placeholderPlayer(), resolvedScope.scope(), metaKey);
        }
        return metaService.getValue(sender, resolvedScope.scope(), metaKey);
    }

    protected Object getDefaultValue(CommandSender sender, String metaKey, MetaCommandTargetResolver.ResolvedMetaTarget target) {
        ResolvedScope resolvedScope = target.resolvedScope();
        if (resolvedScope.placeholderPlayer() != null) {
            return metaService.getDefaultValue(resolvedScope.placeholderPlayer(), metaKey, resolvedScope.scope());
        }
        return metaService.getDefaultValue(sender, metaKey, resolvedScope.scope());
    }

    protected Object setValue(CommandSender sender, String metaKey, Object value, MetaCommandTargetResolver.ResolvedMetaTarget target) {
        ResolvedScope resolvedScope = target.resolvedScope();
        if (resolvedScope.placeholderPlayer() != null) {
            return metaService.setValue(resolvedScope.placeholderPlayer(), resolvedScope.scope(), metaKey, value);
        }
        return metaService.setValue(sender, resolvedScope.scope(), metaKey, value);
    }

    protected Object resetValue(CommandSender sender, String metaKey, MetaCommandTargetResolver.ResolvedMetaTarget target) {
        ResolvedScope resolvedScope = target.resolvedScope();
        if (resolvedScope.placeholderPlayer() != null) {
            return metaService.resetValue(resolvedScope.placeholderPlayer(), resolvedScope.scope(), metaKey);
        }
        return metaService.resetValue(sender, resolvedScope.scope(), metaKey);
    }

    protected Object giveValue(CommandSender sender, String metaKey, String amount, MetaCommandTargetResolver.ResolvedMetaTarget target) {
        ResolvedScope resolvedScope = target.resolvedScope();
        if (resolvedScope.placeholderPlayer() != null) {
            return metaService.giveValue(resolvedScope.placeholderPlayer(), resolvedScope.scope(), metaKey, amount);
        }
        return metaService.giveValue(sender, resolvedScope.scope(), metaKey, amount);
    }

    protected Object takeValue(CommandSender sender, String metaKey, String amount, MetaCommandTargetResolver.ResolvedMetaTarget target) {
        ResolvedScope resolvedScope = target.resolvedScope();
        if (resolvedScope.placeholderPlayer() != null) {
            return metaService.takeValue(resolvedScope.placeholderPlayer(), resolvedScope.scope(), metaKey, amount);
        }
        return metaService.takeValue(sender, resolvedScope.scope(), metaKey, amount);
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
