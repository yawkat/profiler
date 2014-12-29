package at.yawk.profiler.web.sampling;

import at.yawk.profiler.sampler.Sampler;
import at.yawk.profiler.web.AgentAspect;
import at.yawk.profiler.web.Component;
import at.yawk.profiler.web.Page;
import at.yawk.profiler.web.Path;
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

    private class ForestSnapshot {
        boolean running = forestHandler.isRunning();
        JsonElement tree = forestHandler.dump();
    }
}
