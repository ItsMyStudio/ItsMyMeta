package studio.itsmy.itsmymeta.commands;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Locale;
import java.util.stream.Collectors;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import studio.itsmy.itsmymeta.meta.MetaDefinition;
import org.jetbrains.annotations.Nullable;
import studio.itsmy.itsmymeta.commands.subcommands.GetMetaSubcommand;
import studio.itsmy.itsmymeta.commands.subcommands.GiveMetaSubcommand;
import studio.itsmy.itsmymeta.commands.subcommands.HelpMetaSubcommand;
import studio.itsmy.itsmymeta.commands.subcommands.InfoMetaSubcommand;
import studio.itsmy.itsmymeta.commands.subcommands.ListMetaSubcommand;
import studio.itsmy.itsmymeta.commands.subcommands.MetaSubcommand;
import studio.itsmy.itsmymeta.commands.subcommands.ReloadMetaSubcommand;
import studio.itsmy.itsmymeta.commands.subcommands.ResetMetaSubcommand;
import studio.itsmy.itsmymeta.commands.subcommands.SetMetaSubcommand;
import studio.itsmy.itsmymeta.commands.subcommands.TakeMetaSubcommand;
import studio.itsmy.itsmymeta.message.MessageService;
import studio.itsmy.itsmymeta.meta.MetaType;
import studio.itsmy.itsmymeta.meta.MetaService;
import studio.itsmy.itsmymeta.scope.ScopeProviders;

public final class MetaCommand implements CommandExecutor, TabCompleter {

    private static final List<String> META_KEY_SUBCOMMANDS = List.of("get", "set", "give", "take", "reset", "info");

    private final Map<String, MetaSubcommand> subcommands;
    private final MessageService messages;
    private final MetaService metaService;
    private final ScopeProviders scopeProviders;

    public MetaCommand(MetaService metaService, MessageService messages, Runnable reloadAction) {
        this.subcommands = new LinkedHashMap<>();
        this.messages = messages;
        this.metaService = metaService;
        this.scopeProviders = new ScopeProviders();
        register(new GetMetaSubcommand(metaService, messages));
        register(new SetMetaSubcommand(metaService, messages));
        register(new GiveMetaSubcommand(metaService, messages));
        register(new TakeMetaSubcommand(metaService, messages));
        register(new ResetMetaSubcommand(metaService, messages));
        register(new InfoMetaSubcommand(metaService, messages));
        register(new ListMetaSubcommand(metaService, messages));
        register(new ReloadMetaSubcommand(metaService, messages, reloadAction));
        register(new HelpMetaSubcommand(subcommands, messages));
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        try {
            if (args.length == 0) {
                MetaSubcommand helpCommand = subcommands.get("help");
                if (!sender.hasPermission(helpCommand.permission())) {
                    messages.send(sender, "messages.errors.no-permission");
                    return true;
                }
                return helpCommand.execute(sender, label, args);
            }

            String action = args[0].toLowerCase();
            MetaSubcommand subcommand = subcommands.get(action);
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
                    .map(MetaSubcommand::name)
                    .toList(),
                args[0]
            );
        }

        if (args.length == 2) {
            MetaSubcommand subcommand = subcommands.get(args[0].toLowerCase(Locale.ROOT));
            if (subcommand == null || !sender.hasPermission(subcommand.permission())) {
                return List.of();
            }
            if (!META_KEY_SUBCOMMANDS.contains(subcommand.name())) {
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
            MetaSubcommand subcommand = subcommands.get(args[0].toLowerCase(Locale.ROOT));
            if (subcommand == null || !sender.hasPermission(subcommand.permission()) || !META_KEY_SUBCOMMANDS.contains(subcommand.name())) {
                return List.of();
            }

            MetaDefinition definition;
            try {
                definition = metaService.getDefinition(args[1]);
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

    private void register(MetaSubcommand subcommand) {
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
            .map(MetaSubcommand::name)
            .toList();
    }

    private List<MetaDefinition> getSuggestedDefinitions(String subcommandName) {
        return metaService.getDefinitions().stream()
            .filter(definition -> supportsSubcommand(definition, subcommandName))
            .toList();
    }

    private boolean supportsSubcommand(MetaDefinition definition, String subcommandName) {
        return switch (subcommandName) {
            case "give", "take" -> definition.metaType() == MetaType.NUMBER;
            default -> true;
        };
    }
}
