package at.yawk.profiler.agent;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectStreamClass;

/**
 * @author yawkat
 */
@AgentClass
class AgentObjectInputStream extends ObjectInputStream {
    private final AgentClient agent;

    public AgentObjectInputStream(InputStream in, AgentClient agent) throws IOException {
        super(in);
        this.agent = agent;
    }

    @Override
    protected Class<?> resolveClass(ObjectStreamClass desc) throws IOException, ClassNotFoundException {
        try {
            return agent.getModule(ClassBootstrap.class).getClass(desc.getName());
        } catch (ClassNotFoundException e) {
            try {
                // check primitives
                return super.resolveClass(desc);
            } catch (ClassNotFoundException ignored) {}
            throw e;
        }
    }
}
