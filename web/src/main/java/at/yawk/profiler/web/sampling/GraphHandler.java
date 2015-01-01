package at.yawk.profiler.web.sampling;

import at.yawk.profiler.graph.GraphRenderer;
import at.yawk.profiler.graph.GraphvizRenderer;
import at.yawk.profiler.graph.InteractiveSvgRenderer;
import at.yawk.profiler.sampler.Sampler;
import at.yawk.profiler.sampler.StackGraph;
import at.yawk.profiler.sampler.StackGraphSnapshotCollector;

/**
 * @author yawkat
 */
public class GraphHandler {
    private final StackGraph graph = new StackGraph(true);
    private final StackGraphSnapshotCollector collector = new StackGraphSnapshotCollector(graph);
    private volatile boolean running = false;

    synchronized boolean isRunning() {
        return running;
    }

    synchronized void start(Sampler sampler) {
        sampler.start(collector);
        running = true;
    }

    synchronized void stop(Sampler sampler) {
        sampler.stop(collector);
        running = false;
    }

    synchronized void clear() {
        graph.clear();
    }

    synchronized String toSvg(boolean interactive) {
        GraphRenderer renderer = new GraphvizRenderer();
        if (interactive) { renderer = new InteractiveSvgRenderer(renderer); }

        return renderer.renderSvg(graph);
    }
}
