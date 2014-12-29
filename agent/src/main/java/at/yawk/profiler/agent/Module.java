package at.yawk.profiler.agent;

import java.io.IOException;
import lombok.Getter;

/**
 * @author yawkat
 */
@AgentClass
public abstract class Module<R, S> {
    final String name = getClass().getName();

    @Getter AgentClient agent;
    @Getter short id;

    protected void init() {}

    protected void receive(R object) throws Exception {}

    protected final void send(S object) throws IOException {
        agent.send(name, object);
    }
}
