package at.yawk.profiler.sampler;

/**
 * @author yawkat
 */
public interface SnapshotCollector {
    void push(Snapshot snapshot);
}
