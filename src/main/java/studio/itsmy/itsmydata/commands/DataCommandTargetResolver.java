package studio.itsmy.itsmydata.commands;

import org.bukkit.command.CommandSender;
import studio.itsmy.itsmydata.data.DataDefinition;
import studio.itsmy.itsmydata.scope.CommandResolvedScope;
import studio.itsmy.itsmydata.scope.ResolvedScope;
import studio.itsmy.itsmydata.scope.provider.ScopeProviders;

public final class DataCommandTargetResolver {

    private final ScopeProviders scopeProviders;

    public DataCommandTargetResolver() {
        this.scopeProviders = new ScopeProviders();
    }

    public ResolvedDataTarget resolveForRead(CommandSender sender, DataDefinition definition, String[] args, int targetIndex) {
        return resolve(sender, definition, args, targetIndex, true);
    }

    public ResolvedDataTarget resolveForWrite(CommandSender sender, DataDefinition definition, String[] args, int targetIndex) {
        return resolve(sender, definition, args, targetIndex, true);
    }

    private ResolvedDataTarget resolve(CommandSender sender, DataDefinition definition, String[] args, int targetIndex, boolean requireExplicitTarget) {
        String target = args.length > targetIndex ? args[targetIndex] : null;
        CommandResolvedScope commandResolvedScope = scopeProviders.resolveFromCommand(sender, definition.scopeType(), target, requireExplicitTarget);
        int nextArgIndex = commandResolvedScope.consumedTarget() ? targetIndex + 1 : targetIndex;
        return new ResolvedDataTarget(commandResolvedScope.resolvedScope(), nextArgIndex);
    }

    public record ResolvedDataTarget(ResolvedScope resolvedScope, int nextArgIndex) {
    }
}
