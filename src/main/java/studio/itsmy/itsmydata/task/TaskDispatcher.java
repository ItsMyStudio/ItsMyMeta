package studio.itsmy.itsmydata.task;

import java.util.concurrent.CompletableFuture;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public final class TaskDispatcher {

    private final JavaPlugin plugin;

    public TaskDispatcher(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void runSync(Runnable action) {
        if (Bukkit.isPrimaryThread()) {
            action.run();
            return;
        }
        plugin.getServer().getScheduler().runTask(plugin, action);
    }

    public <T> CompletableFuture<T> callSync(CheckedSupplier<T> supplier) {
        if (Bukkit.isPrimaryThread()) {
            return completeImmediately(supplier);
        }

        CompletableFuture<T> future = new CompletableFuture<>();
        plugin.getServer().getScheduler().runTask(plugin, () -> completeFuture(future, supplier));
        return future;
    }

    public <T> CompletableFuture<T> callAsync(CheckedSupplier<T> supplier) {
        CompletableFuture<T> future = new CompletableFuture<>();
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> completeFuture(future, supplier));
        return future;
    }

    private <T> CompletableFuture<T> completeImmediately(CheckedSupplier<T> supplier) {
        CompletableFuture<T> future = new CompletableFuture<>();
        completeFuture(future, supplier);
        return future;
    }

    private <T> void completeFuture(CompletableFuture<T> future, CheckedSupplier<T> supplier) {
        try {
            future.complete(supplier.get());
        } catch (Throwable throwable) {
            future.completeExceptionally(throwable);
        }
    }

    @FunctionalInterface
    public interface CheckedSupplier<T> {
        T get() throws Exception;
    }
}
