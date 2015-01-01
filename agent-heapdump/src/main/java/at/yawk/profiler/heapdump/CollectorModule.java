package at.yawk.profiler.heapdump;

import at.yawk.profiler.agent.Module;
import com.sun.management.HotSpotDiagnosticMXBean;
import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.lang.management.ManagementFactory;
import java.util.UUID;
import lombok.Value;

/**
 * @author yawkat
 */
class CollectorModule extends Module<CollectorModule.Request, CollectorModule.Reply> {
    private HotSpotDiagnosticMXBean bean;

    private synchronized HotSpotDiagnosticMXBean getBean() throws IOException {
        if (bean == null) {
            bean = ManagementFactory.newPlatformMXBeanProxy(
                    ManagementFactory.getPlatformMBeanServer(),
                    "com.sun.management:type=HotSpotDiagnostic",
                    HotSpotDiagnosticMXBean.class
            );
        }
        return bean;
    }

    @Override
    protected void receive(Request object) throws IOException {
        try {
            getBean().dumpHeap(object.file.getAbsolutePath(), true);
            send(new Reply(object.id, null));
        } catch (IOException e) {
            send(new Reply(object.id, e));
        }
    }

    @Value
    static class Request implements Serializable {
        UUID id;
        File file;
    }

    @Value
    static class Reply implements Serializable {
        UUID request;
        Throwable error;
    }
}
