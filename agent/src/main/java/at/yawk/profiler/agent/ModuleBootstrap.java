package at.yawk.profiler.agent;

/**
 * @author yawkat
 */
@AgentClass
class ModuleBootstrap extends Module<String, Void> {
    @SuppressWarnings("unchecked")
    @Override
    protected void receive(String object) throws Exception {
        Class<?> c = getAgent().getModule(ClassBootstrap.class).getClass(object);
        getAgent().loadModule((Class<? extends Module>) c);
    }
}
