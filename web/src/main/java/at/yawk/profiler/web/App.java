package at.yawk.profiler.web;

import at.yawk.profiler.agent.Agent;
import at.yawk.profiler.attach.AttachmentProvider;
import at.yawk.profiler.attach.VmDescriptor;
import at.yawk.profiler.attach.sun.SunAttachmentProvider;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;

/**
 * @author yawkat
 */
@Slf4j
public class App implements AutoCloseable {
    private final Map<String, AgentWrapper> agents = new HashMap<>();
    private ContextHandler handler;

    public synchronized void openAgent(VmDescriptor descriptor) throws IOException, InterruptedException {
        String id = getId(descriptor);
        if (agents.containsKey(id)) { return; }
        Agent attach = Agent.attach(descriptor);
        AgentWrapper wrapper = new AgentWrapper(this, descriptor, attach);
        agents.put(id, wrapper);
        wrapper.start();
        handler.addContext(wrapper.getHandler());
    }

    void remove(AgentWrapper agent) {
        agents.remove(getId(agent.vm));
        handler.removeContext(agent.getHandler());
    }

    public String getId(VmDescriptor descriptor) {
        return descriptor.getProvider().getShortName() + "/" + descriptor.getPid();
    }

    void start(Server server) throws IOException {
        handler = new ContextHandler("/");
        ComponentScanner.getInstance().loadAspects(AppAspect.class, a -> a.app = this, handler, "");
        server.setRootHandler(handler);

        server.start();
    }

    public AttachmentProvider getDefaultAttachmentProvider() {
        return SunAttachmentProvider.getInstance();
    }

    @Override
    public synchronized void close() {
        for (AgentWrapper agent : agents.values()) {
            try {
                agent.close();
            } catch (Exception e) {
                log.error("Failed to close agent", e);
            }
        }
        agents.clear();
    }
}
