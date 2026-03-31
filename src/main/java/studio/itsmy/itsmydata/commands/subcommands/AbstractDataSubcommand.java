package studio.itsmy.itsmydata.commands.subcommands;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.bukkit.command.CommandSender;
import studio.itsmy.itsmydata.commands.DataCommandTargetResolver;
import studio.itsmy.itsmydata.data.DataDefinition;
import studio.itsmy.itsmydata.data.DataService;
import studio.itsmy.itsmydata.message.MessageService;
import studio.itsmy.itsmydata.scope.ResolvedScope;
import studio.itsmy.itsmydata.scope.ScopeType;
import studio.itsmy.itsmydata.task.TaskDispatcher;

public abstract class AbstractDataSubcommand implements DataSubcommand {

    private final TaskDispatcher taskDispatcher;
    private final Logger logger;
    protected final MessageService messages;
    protected final DataService dataService;
    private final DataCommandTargetResolver targetResolver = new DataCommandTargetResolver();

    protected AbstractDataSubcommand(TaskDispatcher taskDispatcher, Logger logger, DataService dataService, MessageService messages) {
        this.taskDispatcher = taskDispatcher;
        this.logger = logger;
        this.dataService = dataService;
        this.messages = messages;
    }

    protected boolean requireDataKey(CommandSender sender, String label, String[] args, String usage) {
        if (args.length < 2) {
            sendIncorrectCommand(sender, label, usage);
            return false;
        }
        return true;
    }

    protected boolean requireValue(CommandSender sender, String label, String[] args, String usage) {
        if (args.length < 3) {
            sendIncorrectCommand(sender, label, usage);
            return false;
        }
        return true;
    }

    protected boolean requireTarget(CommandSender sender, String label, String usage) {
        sendIncorrectCommand(sender, label, usage);
        return true;
    }

    protected boolean requireValue(CommandSender sender, String label, String usage) {
        sendIncorrectCommand(sender, label, usage);
        return true;
    }

    protected DataCommandTargetResolver.ResolvedDataTarget resolveReadTarget(CommandSender sender, DataDefinition definition, String[] args) {
        return targetResolver.resolveForRead(sender, definition, args, 2);
    }

    protected DataCommandTargetResolver.ResolvedDataTarget resolveWriteTarget(CommandSender sender, DataDefinition definition, String[] args) {
        return targetResolver.resolveForWrite(sender, definition, args, 2);
    }

    protected boolean isTargetRequired(DataDefinition definition) {
        return definition.scopeType() != ScopeType.GLOBAL;
    }

    protected <T> boolean completeAsync(CommandSender sender, CompletableFuture<T> future, Consumer<T> onSuccess) {
        future.whenComplete((result, throwable) -> taskDispatcher.runSync(() -> {
            if (throwable != null) {
                Throwable unwrapped = unwrap(throwable);
                logger.log(Level.SEVERE, "Could not execute /data " + name() + " for " + sender.getName() + ".", unwrapped);
                messages.send(
                    sender,
                    "messages.errors.generic",
                    messages.placeholder("message", unwrapped.getMessage() == null ? unwrapped.getClass().getSimpleName() : unwrapped.getMessage())
                );
                return;
            }
            onSuccess.accept(result);
        }));
        return true;
    }

    private void sendIncorrectCommand(CommandSender sender, String label, String usage) {
        messages.send(
            sender,
            "messages.errors.incorrect-command",
            messages.placeholder("label", label),
            messages.placeholder("usage", usage)
        );
    }

    private Throwable unwrap(Throwable throwable) {
        Throwable current = throwable;
        while (current instanceof CompletionException && current.getCause() != null) {
            current = current.getCause();
        }
        return current;
    }
}
