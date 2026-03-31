package studio.itsmy.itsmydata.task;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.concurrent.CompletableFuture;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

public final class TaskDispatcher {

    private final JavaPlugin plugin;
    private final SchedulerAccess schedulerAccess;

    public TaskDispatcher(JavaPlugin plugin) {
        this.plugin = plugin;
        this.schedulerAccess = SchedulerAccess.create(plugin);
    }

    public void runSync(Runnable action) {
        if (schedulerAccess.canRunImmediately()) {
            action.run();
            return;
        }
        schedulerAccess.runSync(action);
    }

    public <T> CompletableFuture<T> callSync(CheckedSupplier<T> supplier) {
        if (schedulerAccess.canRunImmediately()) {
            return completeImmediately(supplier);
        }

        CompletableFuture<T> future = new CompletableFuture<>();
        schedulerAccess.runSync(() -> completeFuture(future, supplier));
        return future;
    }

    public <T> CompletableFuture<T> callAsync(CheckedSupplier<T> supplier) {
        CompletableFuture<T> future = new CompletableFuture<>();
        schedulerAccess.runAsync(() -> completeFuture(future, supplier));
        return future;
    }

    public CancellableTask runGlobalRepeating(Runnable action, long initialDelayTicks, long periodTicks) {
        return schedulerAccess.runGlobalRepeating(action, initialDelayTicks, periodTicks);
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

    public interface CancellableTask {
        void cancel();
    }

    private interface SchedulerAccess {

        static SchedulerAccess create(JavaPlugin plugin) {
            try {
                Method globalSchedulerMethod = plugin.getServer().getClass().getMethod("getGlobalRegionScheduler");
                Method asyncSchedulerMethod = plugin.getServer().getClass().getMethod("getAsyncScheduler");
                Object globalScheduler = globalSchedulerMethod.invoke(plugin.getServer());
                Object asyncScheduler = asyncSchedulerMethod.invoke(plugin.getServer());
                return new FoliaSchedulerAccess(plugin, globalScheduler, asyncScheduler);
            } catch (NoSuchMethodException ignored) {
                return new BukkitSchedulerAccess(plugin);
            } catch (IllegalAccessException | InvocationTargetException exception) {
                throw new IllegalStateException("Could not initialize scheduler access.", exception);
            }
        }

        boolean canRunImmediately();

        void runSync(Runnable action);

        void runAsync(Runnable action);

        CancellableTask runGlobalRepeating(Runnable action, long initialDelayTicks, long periodTicks);
    }

    private static final class BukkitSchedulerAccess implements SchedulerAccess {

        private final JavaPlugin plugin;

        private BukkitSchedulerAccess(JavaPlugin plugin) {
            this.plugin = plugin;
        }

        @Override
        public boolean canRunImmediately() {
            return Bukkit.isPrimaryThread();
        }

        @Override
        public void runSync(Runnable action) {
            plugin.getServer().getScheduler().runTask(plugin, action);
        }

        @Override
        public void runAsync(Runnable action) {
            plugin.getServer().getScheduler().runTaskAsynchronously(plugin, action);
        }

        @Override
        public CancellableTask runGlobalRepeating(Runnable action, long initialDelayTicks, long periodTicks) {
            BukkitTask task = plugin.getServer().getScheduler().runTaskTimer(plugin, action, initialDelayTicks, periodTicks);
            return task::cancel;
        }
    }

    private static final class FoliaSchedulerAccess implements SchedulerAccess {

        private final JavaPlugin plugin;
        private final Object globalScheduler;
        private final Object asyncScheduler;
        private final Method globalExecuteMethod;
        private final Method globalRunAtFixedRateMethod;
        private final Method asyncRunNowMethod;
        private final Method cancelMethod;

        private FoliaSchedulerAccess(JavaPlugin plugin, Object globalScheduler, Object asyncScheduler) {
            this.plugin = plugin;
            this.globalScheduler = globalScheduler;
            this.asyncScheduler = asyncScheduler;
            try {
                this.globalExecuteMethod = globalScheduler.getClass().getMethod("execute", Plugin.class, Runnable.class);
                this.globalRunAtFixedRateMethod = globalScheduler.getClass().getMethod(
                    "runAtFixedRate",
                    Plugin.class,
                    java.util.function.Consumer.class,
                    long.class,
                    long.class
                );
                this.asyncRunNowMethod = asyncScheduler.getClass().getMethod(
                    "runNow",
                    Plugin.class,
                    java.util.function.Consumer.class
                );
                Class<?> scheduledTaskClass = Class.forName("io.papermc.paper.threadedregions.scheduler.ScheduledTask");
                this.cancelMethod = scheduledTaskClass.getMethod("cancel");
            } catch (ClassNotFoundException | NoSuchMethodException exception) {
                throw new IllegalStateException("Folia scheduler API is unavailable.", exception);
            }
        }

        @Override
        public boolean canRunImmediately() {
            return false;
        }

        @Override
        public void runSync(Runnable action) {
            invoke(globalExecuteMethod, globalScheduler, plugin, action);
        }

        @Override
        public void runAsync(Runnable action) {
            invoke(asyncRunNowMethod, asyncScheduler, plugin, (java.util.function.Consumer<Object>) ignored -> action.run());
        }

        @Override
        public CancellableTask runGlobalRepeating(Runnable action, long initialDelayTicks, long periodTicks) {
            Object scheduledTask = invoke(
                globalRunAtFixedRateMethod,
                globalScheduler,
                plugin,
                (java.util.function.Consumer<Object>) ignored -> action.run(),
                initialDelayTicks,
                periodTicks
            );
            return () -> invoke(cancelMethod, scheduledTask);
        }

        private Object invoke(Method method, Object target, Object... args) {
            try {
                return method.invoke(target, args);
            } catch (IllegalAccessException | InvocationTargetException exception) {
                throw new IllegalStateException("Could not invoke Folia scheduler method.", exception);
            }
        }
    }
}
