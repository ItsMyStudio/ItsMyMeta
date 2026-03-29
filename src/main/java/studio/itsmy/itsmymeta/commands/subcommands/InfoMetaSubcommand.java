package studio.itsmy.itsmymeta.commands.subcommands;

import java.util.List;
import org.bukkit.command.CommandSender;
import studio.itsmy.itsmymeta.message.MessageService;
import studio.itsmy.itsmymeta.meta.MetaDefinition;
import studio.itsmy.itsmymeta.commands.MetaCommandTargetResolver;
import studio.itsmy.itsmymeta.meta.MetaService;

public final class InfoMetaSubcommand extends AbstractMetaSubcommand {

    public InfoMetaSubcommand(MetaService metaService, MessageService messages) {
        super(metaService, messages);
    }

    @Override
    public String name() {
        return "info";
    }

    @Override
    public String permission() {
        return "itsmymeta.command.info";
    }

    @Override
    public String usage() {
        return "info <metaKey> [target]";
    }

    @Override
    public String descriptionKey() {
        return "messages.descriptions.info";
    }

    @Override
    public boolean execute(CommandSender sender, String label, String[] args) {
        if (!requireMetaKey(sender, label, args, usage())) {
            return true;
        }

        String metaKey = args[1];
        MetaDefinition definition = metaService.getDefinition(metaKey);
        MetaCommandTargetResolver.ResolvedMetaTarget target = resolveReadTarget(sender, definition, args);
        messages.sendList(
            sender,
            "messages.commands.info",
            messages.placeholder("meta_key", metaKey),
            messages.placeholder("meta_type", definition.metaType().name().toLowerCase()),
            messages.placeholder("scope", definition.scopeType().name().toLowerCase()),
            messages.placeholder("default", getDefaultValue(sender, metaKey, target)),
            messages.placeholder("min", definition.minValue() == null ? messages.raw("messages.common.none") : definition.minValue()),
            messages.placeholder("max", definition.maxValue() == null ? messages.raw("messages.common.none") : definition.maxValue())
        );
        return true;
    }
}
