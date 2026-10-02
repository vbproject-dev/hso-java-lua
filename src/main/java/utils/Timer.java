package utils;



import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * Utility class for managing game timers
 * Provides countdown timers, delayed execution, and repeating tasks
 */
@Slf4j
@UtilityClass
public class Timer {

    private static final ScheduledExecutorService scheduler =
            Executors.newScheduledThreadPool(
                    Runtime.getRuntime().availableProcessors(),
                    r -> {
                        Thread thread = new Thread(r, "GameTimer-Worker");
                        thread.setDaemon(true);
                        return thread;
                    }
            );

    /**
     * Schedule a task to run once after a delay
     *
     * @param task    Task to execute
     * @param delayMs Delay in milliseconds
     * @return TimerHandle to control the timer
     */
    public static TimerHandle schedule(Runnable task, long delayMs) {
        ScheduledFuture<?> future = scheduler.schedule(
                wrapTask(task),
                delayMs,
                TimeUnit.MILLISECONDS
        );
        return new TimerHandle(future, System.currentTimeMillis(), delayMs, false);
    }

    /**
     * Schedule a task to run once after a delay (with time unit)
     */
    public static TimerHandle schedule(Runnable task, long delay, TimeUnit unit) {
        return schedule(task, unit.toMillis(delay));
    }

    /**
     * Schedule a repeating task with fixed delay between executions
     *
     * @param task           Task to execute
     * @param initialDelayMs Initial delay before first execution
     * @param periodMs       Period between executions
     * @return TimerHandle to control the timer
     */
    public static TimerHandle scheduleRepeating(Runnable task, long initialDelayMs, long periodMs) {
        ScheduledFuture<?> future = scheduler.scheduleWithFixedDelay(
                wrapTask(task),
                initialDelayMs,
                periodMs,
                TimeUnit.MILLISECONDS
        );
        return new TimerHandle(future, System.currentTimeMillis(), periodMs, true);
    }

    /**
     * Schedule a repeating task with fixed rate
     * (next execution starts exactly periodMs after previous start)
     */
    public static TimerHandle scheduleAtFixedRate(Runnable task, long initialDelayMs, long periodMs) {
        ScheduledFuture<?> future = scheduler.scheduleAtFixedRate(
                wrapTask(task),
                initialDelayMs,
                periodMs,
                TimeUnit.MILLISECONDS
        );
        return new TimerHandle(future, System.currentTimeMillis(), periodMs, true);
    }

    /**
     * Create a countdown timer with callbacks at each second
     *
     * @param durationMs Total duration in milliseconds
     * @param onTick     Called every second with remaining seconds
     * @param onComplete Called when timer finishes
     * @return TimerHandle to control the countdown
     */
    public static TimerHandle countdown(long durationMs, Consumer<Integer> onTick, Runnable onComplete) {
        long startTime = System.currentTimeMillis();

        TimerHandle[] handleRef = new TimerHandle[1];

        ScheduledFuture<?> future = scheduler.scheduleAtFixedRate(() -> {
            try {
                long elapsed = System.currentTimeMillis() - startTime;
                int remainingSeconds = (int) Math.ceil((durationMs - elapsed) / 1000.0);

                if (remainingSeconds > 0) {
                    if (onTick != null) {
                        onTick.accept(remainingSeconds);
                    }
                } else {
                    // Timer finished
                    if (handleRef[0] != null) {
                        handleRef[0].cancel();
                    }
                    if (onComplete != null) {
                        onComplete.run();
                    }
                }
            } catch (Exception e) {
                log.error("Error in countdown timer", e);
            }
        }, 0, 1000, TimeUnit.MILLISECONDS);

        handleRef[0] = new TimerHandle(future, startTime, durationMs, true);
        return handleRef[0];
    }

    /**
     * Wrap task with error handling
     */
    private static Runnable wrapTask(Runnable task) {
        return () -> {
            try {
                task.run();
            } catch (Exception e) {
                log.error("Error executing timer task", e);
            }
        };
    }

    /**
     * Shutdown the timer scheduler (call on server shutdown)
     */
    public static void shutdown() {
        log.info("Shutting down GameTimer scheduler");
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Handle for controlling a timer
     */
    public record TimerHandle(ScheduledFuture<?> future, long startTime, long durationMs, boolean repeating) {

        /**
         * Cancel this timer
         */
        public void cancel() {
            if (future != null && !future.isDone()) {
                future.cancel(false);
            }
        }

        /**
         * Check if timer is still running
         */
        public boolean isActive() {
            return future != null && !future.isDone() && !future.isCancelled();
        }

        /**
         * Check if timer is cancelled
         */
        public boolean isCancelled() {
            return future != null && future.isCancelled();
        }

        /**
         * Check if timer is completed
         */
        public boolean isDone() {
            return future != null && future.isDone();
        }

        /**
         * Get elapsed time in milliseconds
         */
        public long getElapsedMs() {
            return System.currentTimeMillis() - startTime;
        }

        /**
         * Get remaining time in milliseconds (for non-repeating timers)
         */
        public long getRemainingMs() {
            if (repeating || isDone()) return 0;
            long remaining = durationMs - getElapsedMs();
            return Math.max(0, remaining);
        }

        /**
         * Get remaining time in seconds (for non-repeating timers)
         */
        public int getRemainingSeconds() {
            return (int) Math.ceil(getRemainingMs() / 1000.0);
        }

        /**
         * Get progress percentage (0-100)
         */
        public int getProgressPercent() {
            if (repeating || durationMs == 0) return 0;
            long elapsed = getElapsedMs();
            return (int) Math.min(100, (elapsed * 100) / durationMs);
        }
    }
}