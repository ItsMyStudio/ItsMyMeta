package studio.itsmy.itsmymeta.commands.subcommands;

import org.bukkit.command.CommandSender;
import studio.itsmy.itsmymeta.meta.MetaDefinition;
import studio.itsmy.itsmymeta.message.MessageService;
import studio.itsmy.itsmymeta.meta.MetaService;

public final class ResetMetaSubcommand extends AbstractMetaSubcommand {

    public ResetMetaSubcommand(MetaService metaService, MessageService messages) {
        super(metaService, messages);
    }

    @Override
    public String name() {
        return "reset";
    }

    @Override
    public String permission() {
        return "itsmymeta.command.reset";
    }

    @Override
    public String usage() {
        return "reset <metaKey> [target]";
    }

    @Override
    public String descriptionKey() {
        return "messages.descriptions.reset";
    }

    @Override
    public boolean execute(CommandSender sender, String label, String[] args) {
        if (!requireMetaKey(sender, label, args, usage())) {
            return true;
        }

        String metaKey = args[1];
        MetaDefinition definition = metaService.getDefinition(metaKey);
        var target = resolveReadTarget(sender, definition, args);
        Object resetValue = resetValue(sender, metaKey, target);
        messages.send(
            sender,
            "messages.commands.reset.success",
            messages.placeholder("meta_key", metaKey),
            messages.placeholder("target", target.resolvedScope().displayTarget()),
            messages.placeholder("value", resetValue)
        );
        return true;
    }
}
