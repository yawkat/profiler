package at.yawk.profiler.sampler;

import at.yawk.profiler.agent.Agent;
import java.util.concurrent.ForkJoinPool;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * @author yawkat
 */
@RequiredArgsConstructor
public class Sampler {
    private final Agent agent;
    private boolean attached = false;

    @Getter private final SnapshotCollection snapshots = new SnapshotCollection();

    public synchronized void start() {
        if (!attached) {
            agent.addListener(SamplerModule.class, this::handleSnapshot);

            agent.loadClass(Snapshot.class);
            agent.loadModule(SamplerModule.class);
            attached = true;
        }
        agent.send(SamplerModule.class, true);
    }

    public synchronized void stop() {
        agent.send(SamplerModule.class, false);
    }

    private void handleSnapshot(Snapshot snapshot) {
        ForkJoinPool.commonPool().execute(() -> snapshots.push(snapshot));
    }
}
