package studio.itsmy.itsmymeta.commands.subcommands;

import java.util.Arrays;
import org.bukkit.command.CommandSender;
import studio.itsmy.itsmymeta.message.MessageService;
import studio.itsmy.itsmymeta.meta.MetaDefinition;
import studio.itsmy.itsmymeta.meta.MetaService;
import studio.itsmy.itsmymeta.scope.ScopeType;

public final class GiveMetaSubcommand extends AbstractMetaSubcommand {

    public GiveMetaSubcommand(MetaService metaService, MessageService messages) {
        super(metaService, messages);
    }

    @Override
    public String name() {
        return "give";
    }

    @Override
    public String permission() {
        return "itsmymeta.command.give";
    }

    @Override
    public String usage() {
        return "give <metaKey> [target] <amount>";
    }

    @Override
    public String descriptionKey() {
        return "messages.descriptions.give";
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

        String amount = String.join(" ", Arrays.copyOfRange(args, target.nextArgIndex(), args.length));
        Object updatedValue = giveValue(sender, metaKey, amount, target);
        messages.send(
            sender,
            "messages.commands.give.success",
            messages.placeholder("meta_key", metaKey),
            messages.placeholder("target", target.resolvedScope().displayTarget()),
            messages.placeholder("amount", amount),
            messages.placeholder("value", updatedValue)
        );
        return true;
    }
}
