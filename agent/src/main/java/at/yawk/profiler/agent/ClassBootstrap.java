package at.yawk.profiler.agent;

import java.io.Serializable;
import lombok.Value;

/**
 * @author yawkat
 */
@AgentClass
class ClassBootstrap extends Module<ClassBootstrap.ClassDataWrapper, Void> {
    private final BootstrapCL classLoader = new BootstrapCL();

    @Override
    protected void receive(ClassDataWrapper wrapper) throws Exception {
        classLoader.load(wrapper.name, wrapper.bytes);
    }

    public Class<?> getClass(String name) throws ClassNotFoundException {
        return classLoader.loadClass(name);
    }

    @AgentClass
    private static class BootstrapCL extends ClassLoader {
        void load(String name, byte[] data) {
            Class<?> clazz = defineClass(name, data, 0, data.length);
            resolveClass(clazz);
        }
    }

    @Value
    @AgentClass
    public static class ClassDataWrapper implements Serializable {
        String name;
        byte[] bytes;
    }
}
