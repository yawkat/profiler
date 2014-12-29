package at.yawk.profiler.graph;

import at.yawk.profiler.sampler.StackGraph;

/**
 * @author yawkat
 */
public interface GraphRenderer {
    String renderSvg(StackGraph graph);
}
