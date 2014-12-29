package at.yawk.profiler.agent;

/**
 * @author yawkat
 */
@AgentClass
interface Constants {
    String CHANNEL_BOUND = "$bound";
    String CHANNEL_EXIT = "$exit";
    String PROPERTY_HOST = "host";
    String PROPERTY_PORT = "port";
}
