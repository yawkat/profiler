package at.yawk.profiler.web.misc;

import at.yawk.profiler.heapdump.HeapDumpCollector;
import at.yawk.profiler.web.AgentAspect;
import at.yawk.profiler.web.Component;
import at.yawk.profiler.web.Page;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author yawkat
 */
@Component
public class MiscVmAspect extends AgentAspect {
    @Page(pattern = "index", renderedBy = "vm/index")
    public Object listInfo() {
        return new Info();
    }

    private class Info {
        int pid = getAgent().getVm().getPid();
        String name = getAgent().getVm().getName();
        String provider = getAgent().getVm().getProvider().getShortName();
        List<String> loadedAgentClasses = getAgent().getAgent().getLoadedClasses().stream()
                .sorted().collect(Collectors.toList());
    }

    //// heap dump

    private HeapDumpCollector heapDumpCollector;

    @Page(pattern = "dump_heap")
    public void dumpHeap() throws Throwable {
        synchronized (this) {
            if (heapDumpCollector == null) {
                heapDumpCollector = new HeapDumpCollector(getAgent().getAgent());
            }
        }
        Path target = getAgent().getApp().getHeapDumpManager().createNewDumpTarget();
        heapDumpCollector.dumpHeap(target);
    }

    //// detach

    @Page(pattern = "detach")
    public void detach() throws Exception {
        getAgent().close();
    }
}
