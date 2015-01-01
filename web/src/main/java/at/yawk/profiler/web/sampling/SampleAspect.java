package at.yawk.profiler.web.sampling;

import at.yawk.profiler.sampler.Sampler;
import at.yawk.profiler.web.AgentAspect;
import at.yawk.profiler.web.Component;
import at.yawk.profiler.web.Page;
import at.yawk.profiler.web.Path;
import com.google.common.collect.ImmutableMap;
import com.google.gson.JsonElement;

/**
 * @author yawkat
 */
@Component
@Path("sample/")
public class SampleAspect extends AgentAspect {
    private Sampler sampler;

    private ForestHandler forestHandler = new ForestHandler();

    public synchronized Sampler getSampler() {
        if (sampler == null) { sampler = new Sampler(getAgent().getAgent()); }
        return sampler;
    }

    @Page(pattern = "forest/start")
    public void startForest() {
        forestHandler.start(getSampler());
    }

    @Page(pattern = "forest/stop")
    public void stopForest() {
        forestHandler.stop(getSampler());
    }

    @Page(pattern = "forest/clear")
    public void clearForest() {
        forestHandler.clear();
    }

    @Page(pattern = "forest/snapshot", renderedBy = "vm/sample/forest/snapshot")
    public Object snapshotForest() {
        return new ForestSnapshot();
    }

    @Page(pattern = "forest/index", renderedBy = "vm/sample/forest/index")
    public Object indexForest() {
        return snapshotForest();
    }

    private class ForestSnapshot {
        boolean running = forestHandler.isRunning();
        JsonElement tree = forestHandler.dump();
    }

    private GraphHandler graphHandler = new GraphHandler();

    @Page(pattern = "graph/start")
    public void startGraph() {
        graphHandler.start(getSampler());
    }

    @Page(pattern = "graph/stop")
    public void stopGraph() {
        graphHandler.stop(getSampler());
    }

    @Page(pattern = "graph/clear")
    public void clearGraph() {
        graphHandler.clear();
    }

    @Page(pattern = "graph/snapshot\\.svg", mime = "image/svg+xml")
    public String snapshotGraph() {
        return graphHandler.toSvg(false);
    }

    @Page(pattern = "graph/snapshot\\.svg/interactive", mime = "image/svg+xml")
    public String snapshotGraphInteractive() {
        return graphHandler.toSvg(true);
    }

    @Page(pattern = "graph/index", renderedBy = "vm/sample/graph/index")
    public Object indexGraph() {
        return ImmutableMap.of(
                "running", graphHandler.isRunning()
        );
    }
}
