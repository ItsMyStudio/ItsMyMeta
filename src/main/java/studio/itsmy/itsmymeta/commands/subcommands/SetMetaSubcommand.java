package studio.itsmy.itsmymeta.commands.subcommands;

import java.util.Arrays;
import org.bukkit.command.CommandSender;
import studio.itsmy.itsmymeta.message.MessageService;
import studio.itsmy.itsmymeta.meta.MetaDefinition;
import studio.itsmy.itsmymeta.meta.MetaService;
import studio.itsmy.itsmymeta.scope.ScopeType;

public final class SetMetaSubcommand extends AbstractMetaSubcommand {

    public SetMetaSubcommand(MetaService metaService, MessageService messages) {
        super(metaService, messages);
    }

    @Override
    public String name() {
        return "set";
    }

    @Override
    public String permission() {
        return "itsmymeta.command.set";
    }

    @Override
    public String usage() {
        return "set <metaKey> [target] <value>";
    }

    @Override
    public String descriptionKey() {
        return "messages.descriptions.set";
    }

    @Override
    public boolean execute(CommandSender sender, String label, String[] args) {
        if (!requireMetaKey(sender, label, args, usage())) {
            return true;
        }
        if (!requireValue(sender, label, args, usage())) {
            return true;
        }

        String metaKey = args[1];
        MetaDefinition definition = metaService.getDefinition(metaKey);
        if (definition.scopeType() != ScopeType.GLOBAL && args.length < 4) {
            return requireTarget(sender, label, usage());
        }

        var target = resolveWriteTarget(sender, definition, args);
        if (args.length <= target.nextArgIndex()) {
            return requireValue(sender, label, usage());
        }

        String value = String.join(" ", Arrays.copyOfRange(args, target.nextArgIndex(), args.length));
        Object updatedValue = setValue(sender, metaKey, value, target);
        messages.send(
            sender,
            "messages.commands.set.success",
            messages.placeholder("meta_key", metaKey),
            messages.placeholder("target", target.resolvedScope().displayTarget()),
            messages.placeholder("value", updatedValue)
        );
        return true;
    }
}
