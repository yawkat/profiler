package at.yawk.profiler.heapdump;

import at.yawk.profiler.agent.Agent;
import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Exchanger;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * @author yawkat
 */
@Slf4j
@RequiredArgsConstructor
public class HeapDumpCollector {
    private final Agent agent;
    private boolean attached = false;

    private final Map<UUID, Exchanger<CollectorModule.Reply>> watches = new ConcurrentHashMap<>();

    private synchronized void attach() {
        if (attached) { return; }
        agent.loadModule(CollectorModule.class);
        agent.addListener(CollectorModule.class, f -> {
            Exchanger<CollectorModule.Reply> exchanger = watches.remove(f.getRequest());
            if (exchanger != null) {
                try {
                    exchanger.exchange(f);
                } catch (InterruptedException e) {
                    log.error("Failed to exchange heap dump result to caller", e);
                }
            }
        });
        attached = true;
    }

    public void dumpHeap(Path target) throws Throwable {
        attach();
        Exchanger<CollectorModule.Reply> exchanger = new Exchanger<>();
        UUID uuid = UUID.randomUUID();
        watches.put(uuid, exchanger);
        CollectorModule.Request request = new CollectorModule.Request(uuid, target.toAbsolutePath().toFile());
        agent.send(CollectorModule.class, request);
        CollectorModule.Reply reply = exchanger.exchange(null);
        Throwable exception = reply.getError();
        if (exception != null) { throw exception; }
    }
}
