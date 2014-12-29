package at.yawk.profiler.sampler;

import at.yawk.profiler.agent.Module;
import java.io.IOException;
import java.util.HashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * @author yawkat
 */
class SamplerModule extends Module<Boolean, Snapshot> {
    private static final long INTERVAL = 100;

    private final ScheduledExecutorService timer = Executors.newScheduledThreadPool(0, r -> {
        Thread thread = new Thread(r);
        thread.setName("Timer thread");
        thread.setDaemon(true);
        return thread;
    });

    private ScheduledFuture<?> future;

    @Override
    protected void receive(Boolean object) throws Exception {
        if (object) {
            start();
        } else {
            stop();
        }
    }

    private synchronized void start() {
        stop();
        future = timer.scheduleAtFixedRate(() -> {
            try {
                makeSnapshot();
            } catch (IOException e) {
                getAgent().log(SamplerModule.class, e);
            }
        }, INTERVAL, INTERVAL, TimeUnit.MILLISECONDS);
    }

    private synchronized void stop() {
        if (future != null) {
            future.cancel(false);
            future = null;
        }
    }

    private void makeSnapshot() throws IOException {
        Snapshot snapshot = new Snapshot();
        snapshot.stackTraces = new HashMap<>();
        Thread.getAllStackTraces().forEach((k, v) -> {
            Snapshot.ThreadIdentity threadIdentity = new Snapshot.ThreadIdentity();
            threadIdentity.id = k.getId();
            threadIdentity.name = k.getName();
            threadIdentity.group = k.getThreadGroup() == null ? null : k.getThreadGroup().getName();

            snapshot.stackTraces.put(threadIdentity, v);
        });

        send(snapshot);
    }
}
