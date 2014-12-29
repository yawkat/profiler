package at.yawk.profiler.agent;

import java.lang.instrument.Instrumentation;

/**
 * @author yawkat
 */
@AgentClass
@SuppressWarnings({ "UnusedDeclaration", "SpellCheckingInspection" })
public class Main {
    public static void agentmain(String options, Instrumentation instrumentation) {
        new AgentClient(options, instrumentation).init();
    }

    public static void premain(String options, Instrumentation instrumentation) {
        new AgentClient(options, instrumentation).init();
    }
}
