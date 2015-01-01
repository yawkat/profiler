package at.yawk.profiler.sampler;

import java.util.function.Predicate;

/**
 * @author yawkat
 */
public class StackGraphSnapshotCollector implements SnapshotCollector {
    private final StackGraph graph;
    private final Predicate<Snapshot.ThreadIdentity> threads;

    public StackGraphSnapshotCollector(StackGraph graph, Predicate<Snapshot.ThreadIdentity> threads) {
        this.graph = graph;
        this.threads = threads;
    }

    public StackGraphSnapshotCollector(StackGraph graph) {
        this(graph, t -> true);
    }

    @Override
    public void push(Snapshot snapshot) {
        snapshot.getStackTraces().entrySet().stream()
                .filter(e -> threads.test(e.getKey()))
                .forEach(e -> {
                    synchronized (graph) {
                        graph.push(e.getValue());
                    }
                });
    }
}
