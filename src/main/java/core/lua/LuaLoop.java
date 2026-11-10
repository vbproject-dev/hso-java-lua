package core.lua;

import lombok.extern.slf4j.Slf4j;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
public final class LuaLoop {
    private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor(r -> new Thread(r, "GameLoop"));
    private final AtomicBoolean running = new AtomicBoolean();
    private final int tickRate;
    private long lastTick;

    public LuaLoop(int tickRate) {
        if (tickRate <= 0) throw new IllegalArgumentException("Invalid tick rate");
        this.tickRate = tickRate;
    }

    public void start() {
        if (!running.compareAndSet(false, true)) return;
        lastTick = System.nanoTime();
        executor.scheduleAtFixedRate(() -> {
            long now = System.nanoTime();
            float dt = (float) ((now - lastTick) / 1_000_000_000.0);
            lastTick = now;
            try {
                update(dt);
            } catch (Exception e) {
                log.error("Game loop error", e);
            }
        }, 0, 1_000_000_000L / tickRate, TimeUnit.NANOSECONDS);
    }

    private void update(float dt) {
        if (NativeLua.isLoaded())
            NativeLua.update(dt);
    }

    public void stop() {
        if (!running.compareAndSet(true, false)) return;
        executor.shutdownNow();
    }

    public boolean isRunning() {
        return running.get();
    }
}
