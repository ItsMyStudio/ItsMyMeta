package studio.itsmy.itsmymeta.commands;

import org.bukkit.command.CommandSender;
import studio.itsmy.itsmymeta.meta.MetaDefinition;
import studio.itsmy.itsmymeta.scope.CommandResolvedScope;
import studio.itsmy.itsmymeta.scope.ResolvedScope;
import studio.itsmy.itsmymeta.scope.ScopeProviders;

public final class MetaCommandTargetResolver {

    private final ScopeProviders scopeProviders;

    public MetaCommandTargetResolver() {
        this.scopeProviders = new ScopeProviders();
    }

    public ResolvedMetaTarget resolveForRead(CommandSender sender, MetaDefinition definition, String[] args, int targetIndex) {
        return resolve(sender, definition, args, targetIndex, false);
    }

    public ResolvedMetaTarget resolveForWrite(CommandSender sender, MetaDefinition definition, String[] args, int targetIndex) {
        return resolve(sender, definition, args, targetIndex, true);
    }

    private ResolvedMetaTarget resolve(CommandSender sender, MetaDefinition definition, String[] args, int targetIndex, boolean requireExplicitTarget) {
        String target = args.length > targetIndex ? args[targetIndex] : null;
        CommandResolvedScope commandResolvedScope = scopeProviders.resolveFromCommand(sender, definition.scopeType(), target, requireExplicitTarget);
        int nextArgIndex = commandResolvedScope.consumedTarget() ? targetIndex + 1 : targetIndex;
        return new ResolvedMetaTarget(commandResolvedScope.resolvedScope(), nextArgIndex);
    }

    public record ResolvedMetaTarget(ResolvedScope resolvedScope, int nextArgIndex) {
    }
}
