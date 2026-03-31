package studio.itsmy.itsmydata.commands;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Locale;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import studio.itsmy.itsmydata.data.DataDefinition;
import org.jetbrains.annotations.Nullable;
import studio.itsmy.itsmydata.commands.subcommands.GetDataSubcommand;
import studio.itsmy.itsmydata.commands.subcommands.GiveDataSubcommand;
import studio.itsmy.itsmydata.commands.subcommands.HelpDataSubcommand;
import studio.itsmy.itsmydata.commands.subcommands.InfoDataSubcommand;
import studio.itsmy.itsmydata.commands.subcommands.ListDataSubcommand;
import studio.itsmy.itsmydata.commands.subcommands.DataSubcommand;
import studio.itsmy.itsmydata.commands.subcommands.ReloadDataSubcommand;
import studio.itsmy.itsmydata.commands.subcommands.ResetDataSubcommand;
import studio.itsmy.itsmydata.commands.subcommands.SetDataSubcommand;
import studio.itsmy.itsmydata.commands.subcommands.TakeDataSubcommand;
import studio.itsmy.itsmydata.message.MessageService;
import studio.itsmy.itsmydata.data.DataType;
import studio.itsmy.itsmydata.data.DataService;
import studio.itsmy.itsmydata.scope.provider.ScopeProviders;
import studio.itsmy.itsmydata.task.TaskDispatcher;

public final class DataCommand implements CommandExecutor, TabCompleter {

    private static final List<String> DATA_KEY_SUBCOMMANDS = List.of("get", "set", "give", "take", "reset", "info");

    private final Map<String, DataSubcommand> subcommands = new LinkedHashMap<>();
    private final MessageService messages;
    private final DataService dataService;
    private final ScopeProviders scopeProviders = new ScopeProviders();

    public DataCommand(TaskDispatcher taskDispatcher, Logger logger, DataService dataService, MessageService messages, Runnable reloadAction) {
        this.messages = messages;
        this.dataService = dataService;
        register(new GetDataSubcommand(taskDispatcher, logger, dataService, messages));
        register(new SetDataSubcommand(taskDispatcher, logger, dataService, messages));
        register(new GiveDataSubcommand(taskDispatcher, logger, dataService, messages));
        register(new TakeDataSubcommand(taskDispatcher, logger, dataService, messages));
        register(new ResetDataSubcommand(taskDispatcher, logger, dataService, messages));
        register(new InfoDataSubcommand(taskDispatcher, logger, dataService, messages));
        register(new ListDataSubcommand(taskDispatcher, logger, dataService, messages));
        register(new ReloadDataSubcommand(taskDispatcher, logger, dataService, messages, reloadAction));
        register(new HelpDataSubcommand(subcommands, messages));
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        try {
            if (args.length == 0) {
                DataSubcommand helpCommand = subcommands.get("help");
                if (!sender.hasPermission(helpCommand.permission())) {
                    messages.send(sender, "messages.errors.no-permission");
                    return true;
                }
                return helpCommand.execute(sender, label, args);
            }

            String action = args[0].toLowerCase();
            DataSubcommand subcommand = subcommands.get(action);
            if (subcommand == null) {
                messages.send(
                    sender,
                    "messages.errors.unknown-subcommand",
                    messages.placeholder("input", action),
                    messages.placeholder("subcommands", String.join(", ", getAccessibleSubcommands(sender)))
                );
                return true;
            }
            if (!sender.hasPermission(subcommand.permission())) {
                messages.send(sender, "messages.errors.no-permission");
                return true;
            }

            return subcommand.execute(sender, label, args);
        } catch (IllegalArgumentException | IllegalStateException exception) {
            messages.send(sender, "messages.errors.generic", messages.placeholder("message", exception.getMessage()));
            return true;
        }
    }

    @Override
    public @Nullable List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return filterCompletions(
                subcommands.values().stream()
                    .filter(subcommand -> sender.hasPermission(subcommand.permission()))
                    .map(DataSubcommand::name)
                    .toList(),
                args[0]
            );
        }

        if (args.length == 2) {
            DataSubcommand subcommand = subcommands.get(args[0].toLowerCase(Locale.ROOT));
            if (subcommand == null || !sender.hasPermission(subcommand.permission())) {
                return List.of();
            }
            if (!DATA_KEY_SUBCOMMANDS.contains(subcommand.name())) {
                return List.of();
            }

            return filterCompletions(
                getSuggestedDefinitions(subcommand.name()).stream()
                    .map(definition -> definition.key())
                    .toList(),
                args[1]
            );
        }

        if (args.length == 3) {
            DataSubcommand subcommand = subcommands.get(args[0].toLowerCase(Locale.ROOT));
            if (subcommand == null || !sender.hasPermission(subcommand.permission()) || !DATA_KEY_SUBCOMMANDS.contains(subcommand.name())) {
                return List.of();
            }

            DataDefinition definition;
            try {
                definition = dataService.getDefinition(args[1]);
            } catch (IllegalArgumentException exception) {
                return List.of();
            }

            try {
                return filterCompletions(scopeProviders.get(definition.scopeType()).suggestTargets(), args[2]);
            } catch (IllegalStateException exception) {
                return List.of();
            }
        }

        return List.of();
    }

    private void register(DataSubcommand subcommand) {
        subcommands.put(subcommand.name(), subcommand);
    }

    private List<String> filterCompletions(List<String> values, String input) {
        String loweredInput = input.toLowerCase(Locale.ROOT);
        return values.stream()
            .filter(value -> value.toLowerCase(Locale.ROOT).startsWith(loweredInput))
            .collect(Collectors.toList());
    }

    private List<String> getAccessibleSubcommands(CommandSender sender) {
        return subcommands.values().stream()
            .filter(subcommand -> sender.hasPermission(subcommand.permission()))
            .map(DataSubcommand::name)
            .toList();
    }

    private List<DataDefinition> getSuggestedDefinitions(String subcommandName) {
        return dataService.getDefinitions().stream()
            .filter(definition -> supportsSubcommand(definition, subcommandName))
            .toList();
    }

    private boolean supportsSubcommand(DataDefinition definition, String subcommandName) {
        return switch (subcommandName) {
            case "give", "take" -> definition.dataType() == DataType.NUMBER;
            default -> true;
        };
    }
}
