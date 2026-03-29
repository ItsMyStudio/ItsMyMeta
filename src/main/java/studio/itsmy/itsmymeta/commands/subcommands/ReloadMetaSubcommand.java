package studio.itsmy.itsmymeta.commands.subcommands;

import org.bukkit.command.CommandSender;
import studio.itsmy.itsmymeta.message.MessageService;
import studio.itsmy.itsmymeta.meta.MetaService;

public final class ReloadMetaSubcommand extends AbstractMetaSubcommand {

    private final Runnable reloadAction;

    public ReloadMetaSubcommand(MetaService metaService, MessageService messages, Runnable reloadAction) {
        super(metaService, messages);
        this.reloadAction = reloadAction;
    }

    @Override
    public String name() {
        return "reload";
    }

    @Override
    public String permission() {
        return "itsmymeta.command.reload";
    }

    @Override
    public String usage() {
        return "reload";
    }

    @Override
    public String descriptionKey() {
        return "messages.descriptions.reload";
    }

    @Override
    public boolean execute(CommandSender sender, String label, String[] args) {
        reloadAction.run();
        messages.send(sender, "messages.commands.reload.success");
        return true;
    }
}
