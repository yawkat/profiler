package at.yawk.profiler.sampler;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * @author yawkat
 */
public class SnapshotCollection {
    private final Queue<Snapshot> snapshots = new ConcurrentLinkedQueue<>();

    void push(Snapshot snapshot) {
        snapshot.internStrings();
        snapshots.add(snapshot);
    }

    public void clear() {
        snapshots.clear();
    }

    public StackGraph computeStackGraph(boolean oneNodePerMethod) {
        StackGraph stackGraph = new StackGraph(oneNodePerMethod);
        for (Snapshot snapshot : snapshots) {
            snapshot.stackTraces.forEach((k, v) -> {
                stackGraph.push(v);
            });
        }
        return stackGraph;
    }
}
