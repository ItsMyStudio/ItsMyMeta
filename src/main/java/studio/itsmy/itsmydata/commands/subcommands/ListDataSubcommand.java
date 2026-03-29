package studio.itsmy.itsmydata.commands.subcommands;

import java.util.stream.Collectors;
import java.util.logging.Logger;
import org.bukkit.command.CommandSender;
import studio.itsmy.itsmydata.message.MessageService;
import studio.itsmy.itsmydata.data.DataService;
import studio.itsmy.itsmydata.task.TaskDispatcher;

public final class ListDataSubcommand extends AbstractDataSubcommand {

    public ListDataSubcommand(TaskDispatcher taskDispatcher, Logger logger, DataService dataService, MessageService messages) {
        super(taskDispatcher, logger, dataService, messages);
    }

    @Override
    public String name() {
        return "list";
    }

    @Override
    public String permission() {
        return "itsmydata.command.list";
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
        String dataKeys = dataService.getDefinitions().stream()
            .map(definition -> definition.key())
            .collect(Collectors.joining(", "));

        messages.send(sender, "messages.commands.list.value", messages.placeholder("data_keys", dataKeys));
        return true;
    }
}
