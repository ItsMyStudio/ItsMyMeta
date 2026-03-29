package studio.itsmy.itsmydata.commands.subcommands;

import java.util.logging.Logger;
import org.bukkit.command.CommandSender;
import studio.itsmy.itsmydata.message.MessageService;
import studio.itsmy.itsmydata.data.DataService;
import studio.itsmy.itsmydata.task.TaskDispatcher;

public final class ReloadDataSubcommand extends AbstractDataSubcommand {

    private final Runnable reloadAction;

    public ReloadDataSubcommand(
        TaskDispatcher taskDispatcher,
        Logger logger,
        DataService dataService,
        MessageService messages,
        Runnable reloadAction
    ) {
        super(taskDispatcher, logger, dataService, messages);
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
