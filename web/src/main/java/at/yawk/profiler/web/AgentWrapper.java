package at.yawk.profiler.web;

import at.yawk.profiler.agent.Agent;
import at.yawk.profiler.attach.VmDescriptor;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * @author yawkat
 */
@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
public class AgentWrapper implements AutoCloseable {
    @Getter private final App app;
    final VmDescriptor vm;
    @Getter private final Agent agent;

    @Getter private ContextHandler handler;

    void start() {
        handler = new ContextHandler("vm/" + getApp().getId(vm) + "/");
        ComponentScanner.getInstance().loadAspects(AgentAspect.class, a -> a.agent = this, handler, "vm/");
    }

    @Override
    public void close() throws Exception {
        agent.close();
    }
}
