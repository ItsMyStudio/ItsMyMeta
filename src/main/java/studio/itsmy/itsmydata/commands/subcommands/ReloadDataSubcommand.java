package studio.itsmy.itsmydata.commands.subcommands;

import org.bukkit.command.CommandSender;
import studio.itsmy.itsmydata.message.MessageService;
import studio.itsmy.itsmydata.data.DataService;

public final class ReloadDataSubcommand extends AbstractDataSubcommand {

    private final Runnable reloadAction;

    public ReloadDataSubcommand(DataService dataService, MessageService messages, Runnable reloadAction) {
        super(dataService, messages);
        this.reloadAction = reloadAction;
    }

    @Override
    public String name() {
        return "reload";
    }

    @Override
    public String permission() {
        return "itsmydata.command.reload";
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
