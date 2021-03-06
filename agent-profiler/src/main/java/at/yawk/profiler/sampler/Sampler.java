package at.yawk.profiler.sampler;

import at.yawk.profiler.agent.Agent;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
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

    private final Set<SnapshotCollector> collectors = Collections.newSetFromMap(new ConcurrentHashMap<>());

    public synchronized void start(SnapshotCollector collector) {
        if (!attached) {
            agent.addListener(SamplerModule.class, this::handleSnapshot);

            agent.loadClass(Snapshot.class);
            agent.loadModule(SamplerModule.class);
            attached = true;
        }
        collectors.add(collector);
        if (collectors.size() == 1) {
            agent.send(SamplerModule.class, true);
        }
    }

    public synchronized void stop(SnapshotCollector collector) {
        collectors.remove(collector);
        if (collectors.isEmpty()) {
            agent.send(SamplerModule.class, false);
        }
    }

    private void handleSnapshot(Snapshot snapshot) {
        ForkJoinPool.commonPool().execute(() -> {
            collectors.parallelStream()
                    .forEach(collector -> collector.push(snapshot));
        });
    }
}
