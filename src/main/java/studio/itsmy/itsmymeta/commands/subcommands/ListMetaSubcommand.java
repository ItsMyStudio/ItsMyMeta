package studio.itsmy.itsmymeta.commands.subcommands;

import java.util.stream.Collectors;
import org.bukkit.command.CommandSender;
import studio.itsmy.itsmymeta.message.MessageService;
import studio.itsmy.itsmymeta.meta.MetaService;

public final class ListMetaSubcommand extends AbstractMetaSubcommand {

    public ListMetaSubcommand(MetaService metaService, MessageService messages) {
        super(metaService, messages);
    }

    @Override
    public String name() {
        return "list";
    }

    @Override
    public String permission() {
        return "itsmymeta.command.list";
    }

    @Override
    public String usage() {
        return "list";
    }

    @Override
    public String descriptionKey() {
        return "messages.descriptions.list";
    }

    @Override
    public boolean execute(CommandSender sender, String label, String[] args) {
        String metas = metaService.getDefinitions().stream()
            .map(definition -> definition.key())
            .collect(Collectors.joining(", "));

        messages.send(sender, "messages.commands.list.value", messages.placeholder("metas", metas));
        return true;
    }
}
