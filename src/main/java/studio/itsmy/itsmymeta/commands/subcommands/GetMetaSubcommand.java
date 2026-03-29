package studio.itsmy.itsmymeta.commands.subcommands;

import org.bukkit.command.CommandSender;
import studio.itsmy.itsmymeta.meta.MetaDefinition;
import studio.itsmy.itsmymeta.message.MessageService;
import studio.itsmy.itsmymeta.meta.MetaService;

public final class GetMetaSubcommand extends AbstractMetaSubcommand {

    public GetMetaSubcommand(MetaService metaService, MessageService messages) {
        super(metaService, messages);
    }

    @Override
    public String name() {
        return "get";
    }

    @Override
    public String permission() {
        return "itsmymeta.command.get";
    }

    @Override
    public String usage() {
        return "get <metaKey> [target]";
    }

    @Override
    public String descriptionKey() {
        return "messages.descriptions.get";
    }

    @Override
    public boolean execute(CommandSender sender, String label, String[] args) {
        if (!requireMetaKey(sender, label, args, usage())) {
            return true;
        }

        String metaKey = args[1];
        MetaDefinition definition = metaService.getDefinition(metaKey);
        var target = resolveReadTarget(sender, definition, args);
        Object value = getValue(sender, metaKey, target);
        messages.send(
            sender,
            "messages.commands.get.value",
            messages.placeholder("meta_key", metaKey),
            messages.placeholder("target", target.resolvedScope().displayTarget()),
            messages.placeholder("value", value)
        );
        return true;
    }
}
